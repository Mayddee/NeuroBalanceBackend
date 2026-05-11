package org.example.nbcheckinservice.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nbcheckinservice.dto.GameSessionRequest;
import org.example.nbcheckinservice.dto.GameSessionResponse;
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
@RequestMapping("/new_games")
@RequiredArgsConstructor
@Slf4j
public class NewGameSessionController {
    private final NewGameSessionService gameService;

    private Long getUserId(HttpServletRequest request) {
        return (Long) request.getAttribute("userId");
    }

    @PostMapping("/record")
    public ResponseEntity<GameSessionResponse> recordGameSession(
            HttpServletRequest request,
            @Valid @RequestBody GameSessionRequest gameRequest
    ) {
        Long userId = getUserId(request);
        return ResponseEntity.ok(gameService.recordGameSession(userId, gameRequest));
    }

    /**
     * Получить лучший результат пользователя для конкретной игры
     * GET /games/highscore?type=DONUT_GAME
     */
    @GetMapping("/highscore")
    public ResponseEntity<Map<String, Object>> getHighscore(
            HttpServletRequest request,
            @RequestParam NewGameSession.GameType type
    ) {
        Long userId = getUserId(request);
        Integer best = gameService.getPersonalBest(userId, type);
        return ResponseEntity.ok(Map.of(
                "gameType", type,
                "personalBest", best,
                "unit", (type == NewGameSession.GameType.DONUT_GAME ? "seconds" : "seconds (min is best)")
        ));
    }

    @GetMapping("/today-stats")
    public ResponseEntity<NewGameSessionService.GameStatsResponse> getTodayStats(HttpServletRequest request) {
        return ResponseEntity.ok(gameService.getTodayStats(getUserId(request)));
    }

    @GetMapping("/played-today")
    public ResponseEntity<Boolean> hasPlayedToday(HttpServletRequest request) {
        return ResponseEntity.ok(gameService.hasPlayedToday(getUserId(request)));
    }

    /**
     * Get fun game session history for a specific date (Asia/Almaty)
     * GET /api/v1/new_games/history/date?date=2026-05-11
     */
    @GetMapping("/history/date")
    public ResponseEntity<Map<String, Object>> getSessionsByDate(
            HttpServletRequest request,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        Long userId = getUserId(request);
        LocalDate targetDate = date != null ? date : LocalDate.now(ZoneId.of("Asia/Almaty"));
        log.info("GET /new_games/history/date/{} - User {}", targetDate, userId);
        List<GameSessionResponse> sessions = gameService.getSessionsByDate(userId, targetDate);
        return ResponseEntity.ok(Map.of(
                "date", targetDate.toString(),
                "sessions", sessions,
                "count", sessions.size()
        ));
    }
}
