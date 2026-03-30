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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Fun Games (Gamification)
 * ✅ FIXED: Extract userId from HttpServletRequest
 */
@RestController
@RequestMapping("/games")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Fun Games", description = "Gamification fun games management")
public class GameSessionController {

    private final GameSessionService gameService;

    private Long getUserId(HttpServletRequest request) {
        return (Long) request.getAttribute("userId");
    }

    @PostMapping("/record")
    @Operation(summary = "Record a game session")
    public ResponseEntity<GameSessionResponse> recordGameSession(
            HttpServletRequest request,
            @Valid @RequestBody GameSessionRequest gameRequest
    ) {
        Long userId = getUserId(request);
        log.info("POST /games/record - User {} recording game", userId);

        GameSessionResponse response = gameService.recordGameSession(userId, gameRequest);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/today-stats")
    @Operation(summary = "Get today's game statistics")
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