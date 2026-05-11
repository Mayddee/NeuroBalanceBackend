package org.example.nbcheckinservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nbcheckinservice.dto.BrainGameStatsResponse;
import org.example.nbcheckinservice.dto.BrainGameSubmitRequest;
import org.example.nbcheckinservice.dto.GameResultResponse;
import org.example.nbcheckinservice.entity.BrainGameResult;
import org.example.nbcheckinservice.entity.DailyTask;
import org.example.nbcheckinservice.entity.UserGameStats;
import org.example.nbcheckinservice.kafka.KafkaProducerService;
import org.example.nbcheckinservice.repository.BrainGameResultRepository;
import org.example.nbcheckinservice.repository.UserGameStatsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class BrainGameService {

    private final BrainGameResultRepository gameResultRepository;
    private final UserGameStatsRepository gameStatsRepository;
    private final DailyTaskService dailyTaskService;
    private final KafkaProducerService kafkaProducerService;
    private final UserCharacterService characterService;

    private static final ZoneId ALMATY_ZONE = ZoneId.of("Asia/Almaty");

    @Transactional
    public GameResultResponse submitGameResult(Long userId, BrainGameSubmitRequest request) {

        Integer xpEarned = calculateXP(request);

        BrainGameResult result = BrainGameResult.builder()
                .userId(userId)
                .gameType(request.getGameType())
                .score(request.getScore())
                .timeTakenSeconds(request.getTimeTakenSeconds())
                .xpEarned(xpEarned)
                .isWin(request.getIsWin())
                .difficultyLevel(request.getDifficultyLevel())
                .mistakesCount(request.getMistakesCount())
                .build();

        result = gameResultRepository.save(result);

        UserGameStats stats = gameStatsRepository.findByUserId(userId)
                .orElse(UserGameStats.builder().userId(userId).build());

        stats.incrementGamesPlayed();
        stats.addXp(xpEarned);

        boolean isNewBestTime = false;

        if (request.getIsWin()) {
            stats.recordWin();
            Integer oldBestTime = request.getGameType() == BrainGameResult.GameType.NUMBER_SEQUENCE
                    ? stats.getNumberSequenceBestTime()
                    : stats.getMemoryPairsBestTime();
            stats.updateBestTime(request.getGameType(), request.getTimeTakenSeconds());
            isNewBestTime = oldBestTime == null || request.getTimeTakenSeconds() < oldBestTime;
        } else {
            stats.recordLoss();
        }

        stats.setLastPlayedDate(LocalDate.now(ALMATY_ZONE));
        gameStatsRepository.save(stats);

        // XP сразу зачисляется на персонажа
        characterService.addXp(userId, xpEarned);

        // Auto-complete PLAY_GAME daily task
        dailyTaskService.autoCompleteTask(userId, DailyTask.TaskType.PLAY_GAME);

        // Publish to Kafka for analytics (non-blocking, graceful degradation)
        kafkaProducerService.publishGameCompleted(
                userId,
                request.getGameType().name(),
                request.getDifficultyLevel(),
                request.getIsWin(),
                xpEarned
        );

        String message = generateMessage(request, xpEarned, isNewBestTime);

        log.info("Game result submitted for user {}: {} XP earned, difficulty={}", userId, xpEarned,
                request.getDifficultyLevel());

        return GameResultResponse.builder()
                .resultId(result.getId())
                .xpEarned(xpEarned)
                .totalXp(stats.getTotalXpEarned())
                .isWin(request.getIsWin())
                .message(message)
                .currentStreak(stats.getCurrentStreak())
                .isNewBestTime(isNewBestTime)
                .build();
    }

    public BrainGameStatsResponse getUserStats(Long userId) {

        UserGameStats stats = gameStatsRepository.findByUserId(userId)
                .orElse(UserGameStats.builder().userId(userId).build());

        LocalDate today = LocalDate.now(ALMATY_ZONE);
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

        Long todayGames = gameResultRepository.countTodayGames(userId, startOfDay, endOfDay);
        Integer todayXp = gameResultRepository.sumTodayXp(userId, startOfDay, endOfDay);

        List<BrainGameResult> todayResults = gameResultRepository.findUserGamesInDateRange(userId, startOfDay);
        long todayWins = todayResults.stream().filter(BrainGameResult::getIsWin).count();

        double winRate = stats.getTotalGamesPlayed() > 0
                ? (double) stats.getTotalWins() / stats.getTotalGamesPlayed() * 100
                : 0.0;

        // Weekly stats
        LocalDate weekStart = today.with(java.time.DayOfWeek.MONDAY);
        LocalDateTime wStart = weekStart.atStartOfDay();
        LocalDateTime wEnd = today.atTime(LocalTime.MAX);
        List<BrainGameResult> weeklyGames = gameResultRepository.findUserGamesInRange(userId, wStart, wEnd);
        int weeklyGamesCount = weeklyGames.size();
        long weeklyWins = weeklyGames.stream().filter(BrainGameResult::getIsWin).count();
        int weeklyXp = weeklyGames.stream().mapToInt(BrainGameResult::getXpEarned).sum();

        return BrainGameStatsResponse.builder()
                .totalGamesPlayed(stats.getTotalGamesPlayed())
                .totalXpEarned(stats.getTotalXpEarned())
                .totalWins(stats.getTotalWins())
                .totalLosses(stats.getTotalLosses())
                .winRate(Math.round(winRate * 100.0) / 100.0)
                .todayGamesPlayed(todayGames.intValue())
                .todayXpEarned(todayXp != null ? todayXp : 0)
                .todayWins((int) todayWins)
                .currentStreak(stats.getCurrentStreak())
                .bestStreak(stats.getBestStreak())
                .numberSequenceBestTime(stats.getNumberSequenceBestTime())
                .memoryPairsBestTime(stats.getMemoryPairsBestTime())
                .lastPlayedDate(stats.getLastPlayedDate() != null ? stats.getLastPlayedDate().toString() : null)
                .weeklyGamesPlayed(weeklyGamesCount)
                .weeklyWins((int) weeklyWins)
                .weeklyXpEarned(weeklyXp)
                .build();
    }

    public List<BrainGameResult> getUserGameHistory(Long userId) {
        return gameResultRepository.findByUserIdOrderByPlayedAtDesc(userId);
    }

    public List<BrainGameResult> getUserGameHistoryByType(Long userId, BrainGameResult.GameType gameType) {
        return gameResultRepository.findByUserIdAndGameTypeOrderByPlayedAtDesc(userId, gameType);
    }

    public List<BrainGameResult> getUserGameHistoryByDate(Long userId, LocalDate date) {
        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.atTime(LocalTime.MAX);
        return gameResultRepository.findByUserIdAndDate(userId, dayStart, dayEnd);
    }

    // ========== XP CALCULATION ==========

    /**
     * Многоуровневая формула XP:
     * - Базовый XP: 50 за когнитивную игру (NUMBER_SEQUENCE / MEMORY_PAIRS)
     * - Бонус за скорость: до +50 XP (быстрее → больше)
     * - Бонус за точность: до +50 XP (меньше ошибок → больше)
     * - Множитель сложности: EASY×1, MEDIUM×1.5, HARD×2.5
     * - Поражение: 10 XP (участие, не ноль)
     *
     * Итог: от 10 до 500 XP за игру.
     */
    private Integer calculateXP(BrainGameSubmitRequest request) {

        if (!request.getIsWin()) {
            // Поражение: базовый XP зависит от сложности — сложнее пробовал, больше получил
            int defeatXp = switch (request.getDifficultyLevel() != null ? request.getDifficultyLevel() : "EASY") {
                case "HARD" -> 20;
                case "MEDIUM" -> 15;
                default -> 10;
            };
            return defeatXp;
        }

        int baseXP = 50;

        // Бонус за скорость: макс 50 XP при timeTaken < 30 сек, линейно убывает до 0 при ≥ 150 сек
        int timeSeconds = request.getTimeTakenSeconds() != null ? request.getTimeTakenSeconds() : 150;
        int speedBonus = (int) Math.max(0, 50 * (1.0 - (timeSeconds - 30.0) / 120.0));

        // Бонус за точность: макс 50 XP без ошибок, минус 10 за каждую ошибку
        int mistakes = request.getMistakesCount() != null ? request.getMistakesCount() : 0;
        int accuracyBonus = Math.max(0, 50 - mistakes * 10);

        // Множитель сложности
        double difficultyMultiplier = switch (request.getDifficultyLevel() != null ? request.getDifficultyLevel() : "EASY") {
            case "HARD" -> 2.5;
            case "MEDIUM" -> 1.5;
            default -> 1.0; // EASY
        };

        int rawXP = (int) ((baseXP + speedBonus + accuracyBonus) * difficultyMultiplier);

        return Math.max(10, Math.min(rawXP, 500));
    }

    private String generateMessage(BrainGameSubmitRequest request, Integer xpEarned, boolean isNewBestTime) {

        if (!request.getIsWin()) {
            return "Не сдавайтесь! Попробуйте ещё раз. Вы заработали " + xpEarned + " XP за попытку.";
        }

        if (isNewBestTime) {
            return "🎉 Новый личный рекорд! Вы заработали " + xpEarned + " XP!";
        }

        String diffLabel = switch (request.getDifficultyLevel() != null ? request.getDifficultyLevel() : "EASY") {
            case "HARD" -> "💎 HARD — ";
            case "MEDIUM" -> "⚡ MEDIUM — ";
            default -> "";
        };

        if (xpEarned >= 300) return diffLabel + "Невероятно! Вы заработали " + xpEarned + " XP!";
        if (xpEarned >= 150) return diffLabel + "Отлично! Вы заработали " + xpEarned + " XP!";
        return diffLabel + "Победа! Вы заработали " + xpEarned + " XP!";
    }
}
