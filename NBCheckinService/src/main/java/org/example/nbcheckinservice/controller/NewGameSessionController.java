package org.example.nbcheckinservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nbcheckinservice.dto.GameSessionResponse;
import org.example.nbcheckinservice.dto.NewGameSessionRequest;
import org.example.nbcheckinservice.entity.NewGameSession;
import org.example.nbcheckinservice.service.NewGameSessionService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/new-game-sessions")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "New Game Sessions (DONUT + NUMBER_SEQUENCE)", description = "DONUT_GAME and NUMBER_SEQUENCE_GAME sessions with XP multiplier, speed/attempts bonuses, and daily limits")
public class NewGameSessionController {
    private final NewGameSessionService gameService;

    private Long getUserId(HttpServletRequest request) {
        return (Long) request.getAttribute("userId");
    }

    @PostMapping
    @Operation(summary = "Record a DONUT_GAME or NUMBER_SEQUENCE_GAME session",
               description = "XP = base × streakMultiplier + flat speed/attempts bonus. Max 3 XP sessions per game type per day. Requires isCompleted=true AND durationSeconds≥20. Optional gameDate (yyyy-MM-dd) to record for a past date.")
    public ResponseEntity<GameSessionResponse> recordGameSession(
            HttpServletRequest request,
            @Valid @RequestBody NewGameSessionRequest gameRequest
    ) {
        Long userId = getUserId(request);
        return ResponseEntity.ok(gameService.recordGameSession(userId, gameRequest));
    }

    @GetMapping("/highscore")
    @Operation(summary = "Get personal best for a game type",
               description = "DONUT_GAME: max duration survived. NUMBER_SEQUENCE_GAME: min completion time.")
    public ResponseEntity<Map<String, Object>> getHighscore(
            HttpServletRequest request,
            @RequestParam NewGameSession.GameType type
    ) {
        Long userId = getUserId(request);
        Integer best = gameService.getPersonalBest(userId, type);
        return ResponseEntity.ok(Map.of(
                "gameType", type,
                "personalBest", best,
                "unit", (type == NewGameSession.GameType.DONUT_GAME ? "seconds (max)" : "seconds (min is best)")
        ));
    }

    @GetMapping("/today-stats")
    @Operation(summary = "Get today's new-game statistics (total, wins, XP, win rate)")
    public ResponseEntity<NewGameSessionService.GameStatsResponse> getTodayStats(HttpServletRequest request) {
        return ResponseEntity.ok(gameService.getTodayStats(getUserId(request)));
    }

    @GetMapping("/played-today")
    @Operation(summary = "Check if user played any new-game session today")
    public ResponseEntity<Boolean> hasPlayedToday(HttpServletRequest request) {
        return ResponseEntity.ok(gameService.hasPlayedToday(getUserId(request)));
    }

    @GetMapping("/history/date")
    @Operation(summary = "Get new-game session history for a specific date",
               description = "Defaults to today (Asia/Almaty) if date not provided. Format: yyyy-MM-dd")
    public ResponseEntity<Map<String, Object>> getSessionsByDate(
            HttpServletRequest request,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        Long userId = getUserId(request);
        LocalDate targetDate = date != null ? date : LocalDate.now(ZoneId.of("Asia/Almaty"));
        log.info("GET /new-game-sessions/history/date/{} - User {}", targetDate, userId);
        List<GameSessionResponse> sessions = gameService.getSessionsByDate(userId, targetDate);
        return ResponseEntity.ok(Map.of(
                "date", targetDate.toString(),
                "sessions", sessions,
                "count", sessions.size()
        ));
    }
}
