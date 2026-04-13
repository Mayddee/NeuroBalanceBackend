package org.example.nbcheckinservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nbcheckinservice.dto.GameSessionRequest;
import org.example.nbcheckinservice.dto.GameSessionResponse;
import org.example.nbcheckinservice.entity.NewGameSession;
import org.example.nbcheckinservice.repository.NewGameSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

    private static final ZoneId ALMATY_ZONE = ZoneId.of("Asia/Almaty");

    @Transactional
    public GameSessionResponse recordGameSession(Long userId, GameSessionRequest request) {
        log.info("Recording {} session for user {}", request.getGameType(), userId);

        double xpMultiplier = rewardService.getActiveXpMultiplier(userId);

        // ВАЖНО: убедись, что в GameSessionRequest тип gameType совпадает с NewGameSession.GameType
        // Если они разные, здесь может потребоваться маппинг:
        // NewGameSession.GameType.valueOf(request.getGameType().name())

        NewGameSession game = NewGameSession.builder()
                .userId(userId)
                .gameType(NewGameSession.GameType.valueOf(request.getGameType().name()))
                .durationSeconds(request.getDurationSeconds())
                .isCompleted(request.getIsCompleted())
                .isWon(request.getIsWon())
                .playedAt(LocalDateTime.now(ALMATY_ZONE))
                .build();

        game.calculateXpWithMultiplier(xpMultiplier);
        NewGameSession savedGame = gameRepository.save(game);

        characterService.addXp(userId, game.getXpEarned());
        characterService.increaseHappiness(userId, 5);
        taskService.autoCompleteTask(userId, org.example.nbcheckinservice.entity.DailyTask.TaskType.PLAY_GAME);

        return buildGameResponse(savedGame);
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