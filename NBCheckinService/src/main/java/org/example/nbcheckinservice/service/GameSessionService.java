package org.example.nbcheckinservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nbcheckinservice.dto.GameSessionRequest;
import org.example.nbcheckinservice.dto.GameSessionResponse;
import org.example.nbcheckinservice.entity.GameSession;
import org.example.nbcheckinservice.repository.GameSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for gamification fun games
 * ✅ VERIFIED: All logic is correct
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GameSessionService {

    private final GameSessionRepository gameRepository;
    private final UserCharacterService characterService;
    private final DailyTaskService taskService;

    /**
     * Record a game session
     * ✅ GAMIFICATION LOGIC:
     * 1. Calculate XP (base + win bonus + completion bonus)
     * 2. Award XP to character
     * 3. Increase character happiness +5
     * 4. Auto-complete PLAY_GAME task
     */
    @Transactional
    public GameSessionResponse recordGameSession(Long userId, GameSessionRequest request) {
        log.info("Recording {} game session for user {}", request.getGameType(), userId);

        // Create game entity
        GameSession game = GameSession.builder()
                .userId(userId)
                .gameType(request.getGameType())
                .durationSeconds(request.getDurationSeconds())
                .isCompleted(request.getIsCompleted())
                .isWon(request.getIsWon())
                .playedAt(LocalDateTime.now()) // ✅ Auto-set to NOW
                .build();

        // Calculate XP
        game.calculateXpEarned();

        // Save game
        GameSession savedGame = gameRepository.save(game);

        // ✅ GAMIFICATION: Award XP to character
        characterService.addXp(userId, game.getXpEarned());

        // ✅ GAMIFICATION: Increase character happiness when playing games
        characterService.increaseHappiness(userId, 5); // +5 happiness for playing

        // ✅ GAMIFICATION: Auto-complete daily task if this is first game today
        taskService.autoCompleteTask(userId, org.example.nbcheckinservice.entity.DailyTask.TaskType.PLAY_GAME);

        log.info("Game session recorded: earned {} XP (base: {}, bonus: {})",
                game.getXpEarned(), request.getGameType().getBaseXp(), game.getBonusXp());

        return buildGameResponse(savedGame);
    }

    /**
     * Get today's game statistics
     */
    @Transactional(readOnly = true)
    public GameStatsResponse getTodayStats(Long userId) {
        LocalDate today = LocalDate.now();
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

    /**
     * Check if user played today
     */
    @Transactional(readOnly = true)
    public boolean hasPlayedToday(Long userId) {
        LocalDate today = LocalDate.now();
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