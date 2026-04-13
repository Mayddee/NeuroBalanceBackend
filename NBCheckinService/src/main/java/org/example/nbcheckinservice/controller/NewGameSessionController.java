package org.example.nbcheckinservice.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nbcheckinservice.dto.GameSessionRequest;
import org.example.nbcheckinservice.dto.GameSessionResponse;
import org.example.nbcheckinservice.entity.GameSession;
import org.example.nbcheckinservice.entity.NewGameSession;
import org.example.nbcheckinservice.service.NewGameSessionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}
