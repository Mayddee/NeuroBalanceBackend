package org.example.nbcheckinservice.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nbcheckinservice.dto.BrainGameStatsResponse;
import org.example.nbcheckinservice.dto.BrainGameSubmitRequest;
import org.example.nbcheckinservice.dto.GameResultResponse;
import org.example.nbcheckinservice.entity.BrainGameResult;
import org.example.nbcheckinservice.service.BrainGameService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/brain-games")
@RequiredArgsConstructor
@Slf4j
public class BrainGameController {

    private final BrainGameService brainGameService;

    /**
     * POST /api/v1/brain-games/submit
     * Header: Authorization: Bearer <jwt>
     * Когнитивные игры: NUMBER_SEQUENCE или MEMORY_PAIRS
     */
    @PostMapping("/submit")
    public ResponseEntity<?> submitGameResult(
            HttpServletRequest request,
            @Valid @RequestBody BrainGameSubmitRequest body
    ) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized: missing JWT token"));
        }
        log.info("📥 Game result submission from user {}: {}", userId, body.getGameType());
        return ResponseEntity.ok(brainGameService.submitGameResult(userId, body));
    }

    /**
     * GET /api/v1/brain-games/stats
     * Header: Authorization: Bearer <jwt>
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getUserStats(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized: missing JWT token"));
        }
        log.info("📊 Fetching brain game stats for user {}", userId);
        return ResponseEntity.ok(brainGameService.getUserStats(userId));
    }

    /**
     * GET /api/v1/brain-games/history
     * Header: Authorization: Bearer <jwt>
     * Param: gameType (optional) — NUMBER_SEQUENCE | MEMORY_PAIRS
     */
    @GetMapping("/history")
    public ResponseEntity<?> getUserGameHistory(
            HttpServletRequest request,
            @RequestParam(required = false) BrainGameResult.GameType gameType
    ) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized: missing JWT token"));
        }
        log.info("📜 Fetching brain game history for user {}", userId);
        List<BrainGameResult> history = (gameType != null)
                ? brainGameService.getUserGameHistoryByType(userId, gameType)
                : brainGameService.getUserGameHistory(userId);
        return ResponseEntity.ok(history);
    }
}
