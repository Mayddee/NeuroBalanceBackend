package org.example.nbcheckinservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nbcheckinservice.dto.BrainGameStatsResponse;
import org.example.nbcheckinservice.dto.BrainGameSubmitRequest;
import org.example.nbcheckinservice.dto.GameResultResponse;
import org.example.nbcheckinservice.entity.BrainGameResult;
import org.example.nbcheckinservice.entity.UserGameStats;
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

    // Константа для часового пояса Алматы (+5)
    private static final ZoneId ALMATY_ZONE = ZoneId.of("Asia/Almaty");

    @Transactional
    public GameResultResponse submitGameResult(Long userId, BrainGameSubmitRequest request) {

        // 1. Вычисляем XP
        Integer xpEarned = calculateXP(request);

        // 2. Сохраняем результат игры
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

        // 3. Обновляем статистику пользователя
        UserGameStats stats = gameStatsRepository.findByUserId(userId)
                .orElse(UserGameStats.builder()
                        .userId(userId)
                        .build());

        stats.incrementGamesPlayed();
        stats.addXp(xpEarned);

        boolean isNewBestTime = false;

        if (request.getIsWin()) {
            stats.recordWin();

            // Обновляем лучшее время
            Integer oldBestTime = request.getGameType() == BrainGameResult.GameType.NUMBER_SEQUENCE
                    ? stats.getNumberSequenceBestTime()
                    : stats.getMemoryPairsBestTime();

            stats.updateBestTime(request.getGameType(), request.getTimeTakenSeconds());

            isNewBestTime = oldBestTime == null || request.getTimeTakenSeconds() < oldBestTime;
        } else {
            stats.recordLoss();
        }

        // Устанавливаем дату последнего прохождения по Алматы
        stats.setLastPlayedDate(LocalDate.now(ALMATY_ZONE));

        gameStatsRepository.save(stats);

        // 4. Генерируем сообщение
        String message = generateMessage(request, xpEarned, isNewBestTime);

        log.info("🎮 Game result submitted for user {}: {} XP earned", userId, xpEarned);

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
                .orElse(UserGameStats.builder()
                        .userId(userId)
                        .build());

        // --- ЛОГИКА ТАЙМЗОНЫ ДЛЯ СТАТИСТИКИ ЗА СЕГОДНЯ ---
        LocalDate today = LocalDate.now(ALMATY_ZONE);
        LocalDateTime startOfDay = today.atStartOfDay(); // 00:00:00
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX); // 23:59:59

        // Статистика за сегодня с использованием границ дня
        Long todayGames = gameResultRepository.countTodayGames(userId, startOfDay, endOfDay);
        Integer todayXp = gameResultRepository.sumTodayXp(userId, startOfDay, endOfDay);

        List<BrainGameResult> todayResults = gameResultRepository.findUserGamesInDateRange(
                userId,
                startOfDay
        );

        long todayWins = todayResults.stream().filter(BrainGameResult::getIsWin).count();

        // Win rate
        double winRate = stats.getTotalGamesPlayed() > 0
                ? (double) stats.getTotalWins() / stats.getTotalGamesPlayed() * 100
                : 0.0;

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
                .lastPlayedDate(stats.getLastPlayedDate() != null
                        ? stats.getLastPlayedDate().toString()
                        : null)
                .build();
    }

    public List<BrainGameResult> getUserGameHistory(Long userId) {
        return gameResultRepository.findByUserIdOrderByPlayedAtDesc(userId);
    }

    public List<BrainGameResult> getUserGameHistoryByType(
            Long userId,
            BrainGameResult.GameType gameType
    ) {
        return gameResultRepository.findByUserIdAndGameTypeOrderByPlayedAtDesc(userId, gameType);
    }

    /**
     * Вычисление XP на основе результата игры
     */
    private Integer calculateXP(BrainGameSubmitRequest request) {

        if (!request.getIsWin()) {
            return 10; // Минимальный XP даже за поражение
        }

        int baseXP = 50;

        // Бонус за скорость (чем быстрее, тем больше)
        int speedBonus = Math.max(0, (100 - request.getTimeTakenSeconds()) / 2);

        // Бонус за точность (меньше ошибок = больше XP)
        int accuracyBonus = Math.max(0, 100 - (request.getMistakesCount() * 5));

        // Бонус за сложность
        int difficultyMultiplier = switch (request.getDifficultyLevel()) {
            case "EASY" -> 1;
            case "HARD" -> 2;
            default -> 1; // MEDIUM
        };

        int totalXP = (baseXP + speedBonus + accuracyBonus / 4) * difficultyMultiplier;

        return Math.max(10, Math.min(totalXP, 500)); // Ограничиваем 10-500 XP
    }

    private String generateMessage(BrainGameSubmitRequest request, Integer xpEarned, boolean isNewBestTime) {

        if (!request.getIsWin()) {
            return "Не сдавайтесь! Попробуйте еще раз. Вы заработали " + xpEarned + " XP за попытку.";
        }

        if (isNewBestTime) {
            return "🎉 Новый личный рекорд! Вы заработали " + xpEarned + " XP!";
        }

        if (xpEarned >= 200) {
            return "🔥 Невероятно! Вы заработали " + xpEarned + " XP!";
        } else if (xpEarned >= 100) {
            return "⚡ Отлично! Вы заработали " + xpEarned + " XP!";
        } else {
            return "✅ Победа! Вы заработали " + xpEarned + " XP!";
        }
    }
}