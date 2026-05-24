package org.example.nbcheckinservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nbcheckinservice.dto.GameSessionRequest;
import org.example.nbcheckinservice.dto.GameSessionResponse;
import org.example.nbcheckinservice.entity.GameSession;
import org.example.nbcheckinservice.kafka.KafkaProducerService;
import org.example.nbcheckinservice.repository.GameSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameSessionService {

    private static final ZoneId ALMATY_ZONE = ZoneId.of("Asia/Almaty");
    private static final int DAILY_XP_GAME_LIMIT = 3;
    private static final int MIN_DURATION_FOR_XP_SECONDS = 20;

    private final GameSessionRepository gameRepository;
    private final UserCharacterService characterService;
    private final DailyTaskService taskService;
    private final KafkaProducerService kafkaProducerService;
    private final RewardService rewardService;

    @Transactional
    public GameSessionResponse recordGameSession(Long userId, GameSessionRequest request) {
        log.info("Recording {} game session for user {}", request.getGameType(), userId);

        LocalDate gameDate = request.getGameDate() != null
                ? request.getGameDate()
                : LocalDate.now(ALMATY_ZONE);
        LocalDateTime dayStart = gameDate.atStartOfDay();
        LocalDateTime dayEnd = gameDate.plusDays(1).atStartOfDay();
        LocalDateTime playedAt = gameDate.atTime(java.time.LocalTime.now(ALMATY_ZONE));

        long todayCountForType = gameRepository.countByUserIdAndGameTypeAndPlayedAtBetween(
                userId, request.getGameType(), dayStart, dayEnd);

        GameSession game = GameSession.builder()
                .userId(userId)
                .gameType(request.getGameType())
                .durationSeconds(request.getDurationSeconds())
                .isCompleted(request.getIsCompleted())
                .isWon(request.getIsWon())
                .playedAt(playedAt)
                .build();

        boolean meetsMinDuration = request.getDurationSeconds() != null
                && request.getDurationSeconds() >= MIN_DURATION_FOR_XP_SECONDS;
        boolean completedProperly = Boolean.TRUE.equals(request.getIsCompleted()) && meetsMinDuration;
        boolean withinDailyLimit = todayCountForType < DAILY_XP_GAME_LIMIT;

        if (completedProperly && withinDailyLimit) {
            game.calculateXpEarned();
            int durationBonus = durationBonus(request.getGameType(), request.getDurationSeconds());
            int attemptsBonus = attemptsBonus(request.getGameType(), request.getAttemptsCount());
            int extraBonus = durationBonus + attemptsBonus;
            if (extraBonus > 0) {
                game.setXpEarned(game.getXpEarned() + extraBonus);
                game.setBonusXp((game.getBonusXp() != null ? game.getBonusXp() : 0) + extraBonus);
            }
            // Difficulty multiplier applied last (EASY=1.0×, MEDIUM=1.5×, HARD=2.5×)
            double diffMult = difficultyMultiplier(request.getDifficultyLevel());
            if (diffMult != 1.0) {
                game.setXpEarned((int) Math.round(game.getXpEarned() * diffMult));
            }
        } else {
            game.setXpEarned(0);
            game.setBonusXp(0);
        }

        GameSession savedGame = gameRepository.save(game);

        if (game.getXpEarned() > 0) {
            characterService.addXp(userId, game.getXpEarned());
        }
        characterService.increaseHappiness(userId, 5);

        taskService.getTasksForDate(userId, gameDate);
        taskService.autoCompleteTask(userId, org.example.nbcheckinservice.entity.DailyTask.TaskType.PLAY_GAME, gameDate);

        kafkaProducerService.publishGameCompleted(userId,
                request.getGameType().name(), request.getDifficultyLevel(), request.getIsWon(), game.getXpEarned());

        // Check rewards after transaction commits — avoids race condition where consumer reads DB before commit
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                rewardService.checkAndUnlockRewards(userId);
            }
        });

        log.info("Game session recorded for user {}: type={}, difficulty={}, xp={}, duration={}s, attempts={}, limit={}/{}",
                userId, request.getGameType(), request.getDifficultyLevel(),
                game.getXpEarned(), request.getDurationSeconds(), request.getAttemptsCount(),
                todayCountForType + 1, DAILY_XP_GAME_LIMIT);

        return buildGameResponse(savedGame);
    }

    // ========== XP BONUS HELPERS ==========

    private double difficultyMultiplier(String difficulty) {
        if (difficulty == null) return 1.0;
        return switch (difficulty.toUpperCase()) {
            case "MEDIUM" -> 1.5;
            case "HARD"   -> 2.5;
            default       -> 1.0;
        };
    }

    /**
     * Duration bonus by game type.
     * DONUT_GAME: longer survival = more XP.
     * CHARACTER_CARE: more time spent caring = more XP.
     */
    private int durationBonus(GameSession.GameType type, Integer durationSeconds) {
        if (durationSeconds == null) return 0;
        return switch (type) {
            case DONUT_GAME -> {
                if (durationSeconds > 150) yield 60;
                if (durationSeconds > 90)  yield 40;
                if (durationSeconds > 45)  yield 20;
                yield 0;
            }
            case CHARACTER_CARE -> {
                if (durationSeconds > 70) yield 25;
                if (durationSeconds > 40) yield 15;
                if (durationSeconds > 20) yield 5;
                yield 0;
            }
        };
    }

    /**
     * Attempts bonus by game type.
     * CHARACTER_CARE: more care interactions = engagement bonus.
     * DONUT_GAME: attempts don't apply.
     */
    private int attemptsBonus(GameSession.GameType type, Integer attemptsCount) {
        if (attemptsCount == null || type != GameSession.GameType.CHARACTER_CARE) return 0;
        if (attemptsCount >= 3) return 10;
        if (attemptsCount == 2) return 5;
        return 0;
    }

    /**
     * Get today's game statistics
     */
    @Transactional(readOnly = true)
    public GameStatsResponse getTodayStats(Long userId) {
        LocalDate today = LocalDate.now(ALMATY_ZONE);
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();

        List<GameSession> todayGames = gameRepository
                .findByUserIdAndPlayedAtBetweenOrderByPlayedAtDesc(userId, startOfDay, endOfDay);

        int totalGames = todayGames.size();
        int wins = (int) todayGames.stream().filter(GameSession::getIsWon).count();
        int totalXp = todayGames.stream().mapToInt(GameSession::getXpEarned).sum();

        return GameStatsResponse.builder()
                .totalGamesPlayed(totalGames)
                .wins(wins)
                .xpEarned(totalXp)
                .winRate(totalGames > 0 ? (wins * 100.0) / totalGames : 0.0)
                .build();
    }

    /**
     * Get recent games (last 10)
     */
    @Transactional(readOnly = true)
    public List<GameSessionResponse> getRecentGames(Long userId, int limit) {
        List<GameSession> games = gameRepository.findTop10ByUserIdOrderByPlayedAtDesc(userId);

        return games.stream()
                .limit(limit)
                .map(this::buildGameResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<GameSessionResponse> getSessionsByDate(Long userId, LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();
        return gameRepository.findByUserIdAndPlayedAtBetweenOrderByPlayedAtDesc(userId, start, end)
                .stream().map(this::buildGameResponse).collect(Collectors.toList());
    }

    /**
     * Check if user played today
     */
    @Transactional(readOnly = true)
    public boolean hasPlayedToday(Long userId) {
        LocalDate today = LocalDate.now(ALMATY_ZONE);
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();

        return gameRepository.existsByUserIdAndPlayedAtBetween(userId, startOfDay, endOfDay);
    }

    // ========== HELPER METHODS ==========

    private GameSessionResponse buildGameResponse(GameSession game) {
        return GameSessionResponse.builder()
                .id(game.getId())
                .userId(game.getUserId())
                .gameType(game.getGameType())
                .gameTypeName(game.getGameType().getDisplayName())
                .isCompleted(game.getIsCompleted())
                .isWon(game.getIsWon())
                .durationSeconds(game.getDurationSeconds())
                .xpEarned(game.getXpEarned())
                .bonusXp(game.getBonusXp())
                .playedAt(game.getPlayedAt())
                .build();
    }

    // ========== DTO ==========

    @lombok.Data
    @lombok.Builder
    public static class GameStatsResponse {
        private Integer totalGamesPlayed;
        private Integer wins;
        private Integer xpEarned;
        private Double winRate;
    }
}