package org.example.nbcheckinservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nbcheckinservice.dto.GameSessionRequest;
import org.example.nbcheckinservice.dto.GameSessionResponse;
import org.example.nbcheckinservice.service.GameSessionService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for Fun Games (Gamification)
 * ✅ FIXED: Extract userId from HttpServletRequest
 */
@RestController
@RequestMapping("/game-sessions")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Game Sessions (Fun Games)", description = "DONUT_GAME and CHARACTER_CARE sessions with XP, duration bonus, and daily limits")
public class GameSessionController {

    private final GameSessionService gameService;

    private Long getUserId(HttpServletRequest request) {
        return (Long) request.getAttribute("userId");
    }

    @PostMapping
    @Operation(summary = "Record a DONUT_GAME or CHARACTER_CARE session",
               description = "Earns XP based on win/completion/duration/attempts. Max 3 XP sessions per game type per day. " +
                       "Requires isCompleted=true AND durationSeconds≥20. Optional gameDate (yyyy-MM-dd) to record for a past date.")
    public ResponseEntity<GameSessionResponse> recordGameSession(
            HttpServletRequest request,
            @Valid @RequestBody GameSessionRequest gameRequest
    ) {
        Long userId = getUserId(request);
        log.info("POST /game-sessions - User {} recording {}", userId, gameRequest.getGameType());
        return ResponseEntity.ok(gameService.recordGameSession(userId, gameRequest));
    }

    @GetMapping("/history")
    @Operation(summary = "Get session history for a specific date",
               description = "Returns all DONUT_GAME and CHARACTER_CARE sessions for the given date. Defaults to today (Asia/Almaty) if date not provided.")
    public ResponseEntity<Map<String, Object>> getSessionsByDate(
            HttpServletRequest request,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        Long userId = getUserId(request);
        LocalDate targetDate = date != null ? date : LocalDate.now(ZoneId.of("Asia/Almaty"));
        log.info("GET /game-sessions/history?date={} - User {}", targetDate, userId);
        List<GameSessionResponse> sessions = gameService.getSessionsByDate(userId, targetDate);
        return ResponseEntity.ok(Map.of(
                "date", targetDate.toString(),
                "sessions", sessions,
                "count", sessions.size()
        ));
    }

    @GetMapping("/today-stats")
    @Operation(summary = "Get today's game statistics (total games, wins, XP, win rate)")
    public ResponseEntity<GameSessionService.GameStatsResponse> getTodayStats(
            HttpServletRequest request
    ) {
        Long userId = getUserId(request);
        log.info("GET /games/today-stats - User {}", userId);

        GameSessionService.GameStatsResponse stats = gameService.getTodayStats(userId);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/recent")
    @Operation(summary = "Get recent games")
    public ResponseEntity<List<GameSessionResponse>> getRecentGames(
            HttpServletRequest request,
            @RequestParam(defaultValue = "10") int limit
    ) {
        Long userId = getUserId(request);
        log.info("GET /games/recent?limit={} - User {}", limit, userId);

        List<GameSessionResponse> games = gameService.getRecentGames(userId, limit);
        return ResponseEntity.ok(games);
    }

    @GetMapping("/played-today")
    @Operation(summary = "Check if user played today")
    public ResponseEntity<Boolean> hasPlayedToday(HttpServletRequest request) {
        Long userId = getUserId(request);
        log.info("GET /games/played-today - User {}", userId);

        boolean played = gameService.hasPlayedToday(userId);
        return ResponseEntity.ok(played);
    }
}