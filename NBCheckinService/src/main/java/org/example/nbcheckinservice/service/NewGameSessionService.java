package org.example.nbcheckinservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nbcheckinservice.dto.GameSessionResponse;
import org.example.nbcheckinservice.dto.NewGameSessionRequest;
import org.example.nbcheckinservice.entity.NewGameSession;
import org.example.nbcheckinservice.kafka.KafkaProducerService;
import org.example.nbcheckinservice.repository.NewGameSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j // Используем lombok @Slf4j вместо импорта из Kafka
public class NewGameSessionService {

    private final NewGameSessionRepository gameRepository;
    private final UserCharacterService characterService;
    private final DailyTaskService taskService;
    private final RewardService rewardService;
    private final KafkaProducerService kafkaProducerService;

    private static final ZoneId ALMATY_ZONE = ZoneId.of("Asia/Almaty");
    private static final int DAILY_XP_GAME_LIMIT = 3;
    private static final int MIN_DURATION_FOR_XP_SECONDS = 20;

    @Transactional
    public GameSessionResponse recordGameSession(Long userId, NewGameSessionRequest request) {
        log.info("Recording {} session for user {}", request.getGameType(), userId);

        double xpMultiplier = rewardService.getActiveXpMultiplier(userId);

        LocalDate gameDate = request.getGameDate() != null
                ? request.getGameDate()
                : LocalDate.now(ALMATY_ZONE);
        LocalDateTime playedAt = gameDate.atTime(LocalTime.now(ALMATY_ZONE));

        NewGameSession.GameType gameType = request.getGameType(); // already correct enum type

        LocalDateTime dayStart = gameDate.atStartOfDay();
        LocalDateTime dayEnd = gameDate.plusDays(1).atStartOfDay();
        long todayCountForType = gameRepository.countByUserIdAndGameTypeAndPlayedAtBetween(
                userId, gameType, dayStart, dayEnd);

        NewGameSession game = NewGameSession.builder()
                .userId(userId)
                .gameType(gameType)
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
            game.calculateXpWithMultiplier(xpMultiplier);
            // Flat bonuses not subject to streak multiplier
            int durationBonus = durationBonus(gameType, request.getDurationSeconds());
            int attemptsBonus = attemptsBonus(gameType, request.getAttemptsCount());
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

        NewGameSession savedGame = gameRepository.save(game);

        if (game.getXpEarned() > 0) {
            characterService.addXp(userId, game.getXpEarned());
        }
        characterService.increaseHappiness(userId, 5);

        taskService.getTasksForDate(userId, gameDate);
        taskService.autoCompleteTask(userId, org.example.nbcheckinservice.entity.DailyTask.TaskType.PLAY_GAME, gameDate);

        kafkaProducerService.publishGameCompleted(userId,
                gameType.name(), request.getDifficultyLevel(), request.getIsWon(), game.getXpEarned());

        // Check rewards after transaction commits — avoids race condition
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                rewardService.checkAndUnlockRewards(userId);
            }
        });

        log.info("New game session recorded for user {}: type={}, difficulty={}, xp={}, duration={}s, attempts={}, limit={}/{}",
                userId, gameType, request.getDifficultyLevel(),
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
     * Duration bonus by game type:
     * DONUT_GAME: longer survival = more bonus.
     * NUMBER_SEQUENCE_GAME: faster completion = more bonus (cognitive speed reward).
     */
    private int durationBonus(NewGameSession.GameType type, Integer durationSeconds) {
        if (durationSeconds == null) return 0;
        return switch (type) {
            case DONUT_GAME -> {
                if (durationSeconds > 150) yield 60;
                if (durationSeconds > 90)  yield 40;
                if (durationSeconds > 45)  yield 20;
                yield 0;
            }
            case NUMBER_SEQUENCE_GAME -> {
                // Faster = smarter — speed bonus
                if (durationSeconds < 30)  yield 40;
                if (durationSeconds < 60)  yield 25;
                if (durationSeconds < 90)  yield 15;
                yield 0;
            }
        };
    }

    /**
     * Attempts bonus by game type:
     * NUMBER_SEQUENCE_GAME: fewer attempts = higher precision bonus.
     * DONUT_GAME: attempts don't apply.
     */
    private int attemptsBonus(NewGameSession.GameType type, Integer attemptsCount) {
        if (attemptsCount == null || type != NewGameSession.GameType.NUMBER_SEQUENCE_GAME) return 0;
        if (attemptsCount == 1) return 30; // perfect, solved on first try
        if (attemptsCount == 2) return 15;
        if (attemptsCount == 3) return 5;
        return 0; // >3 attempts: no bonus
    }

    @Transactional(readOnly = true)
    public Integer getPersonalBest(Long userId, NewGameSession.GameType gameType) {
        List<NewGameSession> sessions = gameRepository.findByUserIdAndGameType(userId, gameType);
        if (gameType == NewGameSession.GameType.DONUT_GAME) {
            return sessions.stream().mapToInt(NewGameSession::getDurationSeconds).max().orElse(0);
        } else {
            return sessions.stream().filter(NewGameSession::getIsWon)
                    .mapToInt(NewGameSession::getDurationSeconds).min().orElse(0);
        }
    }

    @Transactional(readOnly = true)
    public GameStatsResponse getTodayStats(Long userId) {
        LocalDate today = LocalDate.now(ALMATY_ZONE);
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();

        List<NewGameSession> todayGames = gameRepository.findByUserIdAndPlayedAtBetweenOrderByPlayedAtDesc(userId, start, end);

        int totalGames = todayGames.size();
        int wins = (int) todayGames.stream().filter(NewGameSession::getIsWon).count();
        int totalXp = todayGames.stream().mapToInt(NewGameSession::getXpEarned).sum();

        return GameStatsResponse.builder()
                .totalGamesPlayed(totalGames)
                .wins(wins)
                .xpEarned(totalXp)
                .winRate(totalGames > 0 ? (wins * 100.0) / totalGames : 0.0)
                .build();
    }

    @Transactional(readOnly = true)
    public boolean hasPlayedToday(Long userId) {
        LocalDate today = LocalDate.now(ALMATY_ZONE);
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();
        return gameRepository.existsByUserIdAndPlayedAtBetween(userId, start, end);
    }

    @Transactional(readOnly = true)
    public List<GameSessionResponse> getSessionsByDate(Long userId, LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();
        return gameRepository.findByUserIdAndPlayedAtBetweenOrderByPlayedAtDesc(userId, start, end)
                .stream().map(this::buildGameResponse).collect(java.util.stream.Collectors.toList());
    }

    private GameSessionResponse buildGameResponse(NewGameSession game) {
        return GameSessionResponse.builder()
                .id(game.getId())
                .userId(game.getUserId())
                .gameTypeName(game.getGameType().getDisplayName())
                .isCompleted(game.getIsCompleted())
                .isWon(game.getIsWon())
                .durationSeconds(game.getDurationSeconds())
                .xpEarned(game.getXpEarned())
                .bonusXp(game.getBonusXp())
                .playedAt(game.getPlayedAt())
                .build();
    }

    @lombok.Data @lombok.Builder
    public static class GameStatsResponse {
        private Integer totalGamesPlayed;
        private Integer wins;
        private Integer xpEarned;
        private Double winRate;
    }
}