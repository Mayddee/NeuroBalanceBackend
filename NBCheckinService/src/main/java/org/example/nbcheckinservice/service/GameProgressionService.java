package org.example.nbcheckinservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nbcheckinservice.dto.CharacterResponse;
import org.example.nbcheckinservice.entity.BrainGameResult;
import org.example.nbcheckinservice.entity.UserCharacter;
import org.example.nbcheckinservice.repository.BrainGameResultRepository;
import org.example.nbcheckinservice.repository.UserCharacterRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

/**
 * GameProgressionService — строгая система прогрессии персонажа.
 *
 * Уровни поднимаются ТОЛЬКО через еженедельные гейты когнитивных игр:
 *   Lv1→2: 15 когнитивных игр за неделю (любая сложность), winRate ≥ 50%
 *   Lv2→3: 20 когнитивных игр за неделю, из них ≥ 10 на MEDIUM/HARD, winRate ≥ 55%
 *   Lv3→4: 20 когнитивных игр за неделю, из них ≥ 15 на HARD, winRate ≥ 60%
 *   Lv4→5: 25 когнитивных игр за неделю, все на HARD, winRate ≥ 70%
 *
 * Неделя считается: текущая ISO-неделя (Пн–Вс) по Asia/Almaty.
 * Персонаж не поднимается автоматически — только через явный вызов tryLevelUp().
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GameProgressionService {

    private static final ZoneId ALMATY_ZONE = ZoneId.of("Asia/Almaty");

    private final BrainGameResultRepository gameResultRepository;
    private final UserCharacterRepository characterRepository;
    private final UserCharacterService characterService;

    // ========== LEVEL-UP GATES ==========

    private static final int[] WEEKLY_GAMES_REQUIRED   = {0, 15, 20, 20, 25}; // index = currentLevel
    private static final int[] HARD_GAMES_REQUIRED     = {0,  0, 10, 15, 25};
    private static final double[] WIN_RATE_REQUIRED    = {0, 0.50, 0.55, 0.60, 0.70};
    private static final String[] DIFFICULTY_GATE      = {"", "ANY", "MEDIUM", "HARD", "HARD"};

    /**
     * Попытка повысить уровень персонажа на основе результатов текущей недели.
     * Возвращает CharacterResponse (с justLeveledUp=true если произошёл level-up).
     */
    @Transactional
    public CharacterResponse tryLevelUp(Long userId) {
        UserCharacter character = characterService.getOrCreateCharacter(userId);

        if (character.getCurrentLevel() >= 5) {
            log.info("User {} is already at max level 5", userId);
            return characterService.getCharacterResponse(userId);
        }

        WeeklyStats stats = getWeeklyStats(userId);
        int level = character.getCurrentLevel();

        boolean canLevelUp = checkLevelUpConditions(level, stats);

        if (canLevelUp) {
            log.info("Level-up conditions met for user {} (level {} → {}): games={}, hardGames={}, winRate={}",
                    userId, level, level + 1, stats.totalCognitiveGames, stats.hardGames,
                    String.format("%.0f%%", stats.winRate * 100));

            // Award significant XP burst for completing the weekly challenge
            int levelUpXp = getLevelUpXpReward(level);
            CharacterResponse response = characterService.addXp(userId, levelUpXp);

            log.info("Level-up XP awarded: {} XP for user {}", levelUpXp, userId);
            return response;
        }

        log.info("Level-up conditions NOT met for user {} at level {}: {}", userId, level, stats);
        return characterService.getCharacterResponse(userId);
    }

    /**
     * Возвращает статус прогрессии для фронтенда:
     * сколько игр до гейта, какой win rate нужен и тд.
     */
    @Transactional(readOnly = true)
    public ProgressionStatusResponse getProgressionStatus(Long userId) {
        UserCharacter character = characterService.getOrCreateCharacter(userId);
        int level = character.getCurrentLevel();

        WeeklyStats stats = getWeeklyStats(userId);

        if (level >= 5) {
            return ProgressionStatusResponse.builder()
                    .currentLevel(5)
                    .isMaxLevel(true)
                    .weeklyGamesPlayed(stats.totalCognitiveGames)
                    .weeklyWins(stats.wins)
                    .weeklyWinRate(stats.winRate)
                    .message("Максимальный уровень достигнут! 🏆")
                    .build();
        }

        int gamesRequired = WEEKLY_GAMES_REQUIRED[level];
        int hardRequired = HARD_GAMES_REQUIRED[level];
        double winRequired = WIN_RATE_REQUIRED[level];
        String diffGate = DIFFICULTY_GATE[level];

        boolean gamesOk = stats.totalCognitiveGames >= gamesRequired;
        boolean hardOk = stats.hardGames >= hardRequired;
        boolean winOk = (stats.totalCognitiveGames == 0) || stats.winRate >= winRequired;
        boolean canLevelUp = gamesOk && hardOk && winOk;

        String statusMsg = buildStatusMessage(level, stats, gamesOk, hardOk, winOk, canLevelUp);

        return ProgressionStatusResponse.builder()
                .currentLevel(level)
                .nextLevel(level + 1)
                .isMaxLevel(false)
                .canLevelUp(canLevelUp)
                .weeklyGamesPlayed(stats.totalCognitiveGames)
                .weeklyGamesRequired(gamesRequired)
                .weeklyHardGamesPlayed(stats.hardGames)
                .weeklyHardGamesRequired(hardRequired)
                .weeklyWins(stats.wins)
                .weeklyWinRate(stats.winRate)
                .weeklyWinRateRequired(winRequired)
                .difficultyGate(diffGate)
                .gamesProgress((int) Math.round((stats.totalCognitiveGames * 100.0) / Math.max(gamesRequired, 1)))
                .message(statusMsg)
                .totalXp(character.getTotalXp())
                .xpForNextLevel(character.getXpForNextLevel())
                .levelProgress(character.getLevelProgressPercentage())
                .build();
    }

    // ========== INTERNAL ==========

    private boolean checkLevelUpConditions(int level, WeeklyStats stats) {
        if (level >= 5) return false;

        boolean gamesOk = stats.totalCognitiveGames >= WEEKLY_GAMES_REQUIRED[level];
        boolean hardOk = stats.hardGames >= HARD_GAMES_REQUIRED[level];
        boolean winOk = stats.totalCognitiveGames == 0 || stats.winRate >= WIN_RATE_REQUIRED[level];

        return gamesOk && hardOk && winOk;
    }

    /**
     * XP-вознаграждение за level-up — нарастает с уровнем,
     * так что ранние уровни не дают большого буста.
     */
    private int getLevelUpXpReward(int currentLevel) {
        return switch (currentLevel) {
            case 1 -> 300;   // Lv1→2: стартовый буст
            case 2 -> 600;   // Lv2→3
            case 3 -> 1200;  // Lv3→4
            case 4 -> 2000;  // Lv4→5: максимальный буст
            default -> 0;
        };
    }

    private WeeklyStats getWeeklyStats(Long userId) {
        LocalDate today = LocalDate.now(ALMATY_ZONE);
        // Начало текущей ISO-недели (Понедельник)
        LocalDate weekStart = today.with(java.time.DayOfWeek.MONDAY);
        LocalDate weekEnd = weekStart.plusDays(6);

        LocalDateTime start = weekStart.atStartOfDay();
        LocalDateTime end = weekEnd.atTime(LocalTime.MAX);

        List<BrainGameResult> weekGames = gameResultRepository.findUserGamesInRange(userId, start, end);

        int total = weekGames.size();
        int wins = (int) weekGames.stream().filter(BrainGameResult::getIsWin).count();
        int hardGames = (int) weekGames.stream()
                .filter(g -> "HARD".equalsIgnoreCase(g.getDifficultyLevel()))
                .count();
        int mediumPlusGames = (int) weekGames.stream()
                .filter(g -> "MEDIUM".equalsIgnoreCase(g.getDifficultyLevel())
                        || "HARD".equalsIgnoreCase(g.getDifficultyLevel()))
                .count();
        double winRate = total > 0 ? (wins * 1.0) / total : 0.0;

        return new WeeklyStats(total, wins, hardGames, mediumPlusGames, winRate, weekStart, weekEnd);
    }

    private String buildStatusMessage(int level, WeeklyStats stats, boolean gamesOk, boolean hardOk,
                                       boolean winOk, boolean canLevelUp) {
        if (canLevelUp) {
            return "🎉 Условия выполнены! Нажми 'Level Up' чтобы повысить уровень персонажа!";
        }
        if (!gamesOk) {
            int needed = WEEKLY_GAMES_REQUIRED[level] - stats.totalCognitiveGames;
            return String.format("Нужно ещё %d когнитивных игр на этой неделе 🎮", needed);
        }
        if (!hardOk) {
            int needed = HARD_GAMES_REQUIRED[level] - stats.hardGames;
            return String.format("Нужно ещё %d игр на уровне HARD 💪", needed);
        }
        if (!winOk) {
            return String.format("Нужен win rate ≥ %.0f%% (сейчас %.0f%%) ⚡",
                    WIN_RATE_REQUIRED[level] * 100, stats.winRate * 100);
        }
        return "Продолжай играть! 🚀";
    }

    // ========== INNER CLASSES ==========

    private record WeeklyStats(
            int totalCognitiveGames,
            int wins,
            int hardGames,
            int mediumPlusGames,
            double winRate,
            LocalDate weekStart,
            LocalDate weekEnd
    ) {}

    @lombok.Data
    @lombok.Builder
    public static class ProgressionStatusResponse {
        private Integer currentLevel;
        private Integer nextLevel;
        private Boolean isMaxLevel;
        private Boolean canLevelUp;
        private Integer weeklyGamesPlayed;
        private Integer weeklyGamesRequired;
        private Integer weeklyHardGamesPlayed;
        private Integer weeklyHardGamesRequired;
        private Integer weeklyWins;
        private Double weeklyWinRate;
        private Double weeklyWinRateRequired;
        private String difficultyGate;
        private Integer gamesProgress; // 0-100 %
        private String message;
        private Integer totalXp;
        private Integer xpForNextLevel;
        private Double levelProgress;
    }
}
