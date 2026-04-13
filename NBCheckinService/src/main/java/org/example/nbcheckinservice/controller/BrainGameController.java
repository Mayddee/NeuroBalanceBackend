package org.example.nbcheckinservice.controller;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nbcheckinservice.dto.BrainGameStatsResponse;
import org.example.nbcheckinservice.dto.BrainGameSubmitRequest;
import org.example.nbcheckinservice.dto.GameResultResponse;
import org.example.nbcheckinservice.entity.BrainGameResult;
import org.example.nbcheckinservice.service.BrainGameService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/brain-games")
@RequiredArgsConstructor
@Slf4j
public class BrainGameController {

    private final BrainGameService brainGameService;

    /**
     * Отправка результата игры
     *
     * POST /api/v1/brain-games/submit
     * Header: X-User-Id: 1
     * Body: {
     *   "gameType": "NUMBER_SEQUENCE",
     *   "score": 100,
     *   "timeTakenSeconds": 45,
     *   "isWin": true,
     *   "difficultyLevel": "MEDIUM",
     *   "mistakesCount": 2
     * }
     */
    @PostMapping("/submit")
    public ResponseEntity<GameResultResponse> submitGameResult(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody BrainGameSubmitRequest request
    ) {
        log.info("📥 Game result submission from user {}: {}", userId, request.getGameType());

        GameResultResponse response = brainGameService.submitGameResult(userId, request);

        return ResponseEntity.ok(response);
    }

    /**
     * Получить статистику пользователя
     *
     * GET /api/v1/brain-games/stats
     * Header: X-User-Id: 1
     */
    @GetMapping("/stats")
    public ResponseEntity<BrainGameStatsResponse> getUserStats(
            @RequestHeader("X-User-Id") Long userId
    ) {
        log.info("📊 Fetching stats for user {}", userId);

        BrainGameStatsResponse stats = brainGameService.getUserStats(userId);

        return ResponseEntity.ok(stats);
    }

    /**
     * История игр пользователя
     *
     * GET /api/v1/brain-games/history
     * Header: X-User-Id: 1
     */
    @GetMapping("/history")
    public ResponseEntity<List<BrainGameResult>> getUserGameHistory(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(required = false) BrainGameResult.GameType gameType
    ) {
        log.info("📜 Fetching game history for user {}", userId);

        List<BrainGameResult> history;

        if (gameType != null) {
            history = brainGameService.getUserGameHistoryByType(userId, gameType);
        } else {
            history = brainGameService.getUserGameHistory(userId);
        }

        return ResponseEntity.ok(history);
    }
}
