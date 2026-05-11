package org.example.nbcheckinservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nbcheckinservice.dto.BrainGameStatsResponse;
import org.example.nbcheckinservice.dto.BrainGameSubmitRequest;
import org.example.nbcheckinservice.dto.CharacterResponse;
import org.example.nbcheckinservice.dto.GameResultResponse;
import org.example.nbcheckinservice.entity.BrainGameResult;
import org.example.nbcheckinservice.service.BrainGameService;
import org.example.nbcheckinservice.service.GameProgressionService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/brain-games")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Brain Games", description = "Cognitive game submission and analytics")
public class BrainGameController {

    private final BrainGameService brainGameService;
    private final GameProgressionService gameProgressionService;

    private static final ZoneId ALMATY_ZONE = ZoneId.of("Asia/Almaty");

    private Long getUserId(HttpServletRequest request) {
        return (Long) request.getAttribute("userId");
    }

    /**
     * Submit a cognitive game result (NUMBER_SEQUENCE or MEMORY_PAIRS).
     * Awards XP immediately to the character and marks PLAY_GAME task complete.
     *
     * POST /api/v1/brain-games/submit
     */
    @PostMapping("/submit")
    @Operation(summary = "Submit brain game result")
    public ResponseEntity<?> submitGameResult(
            HttpServletRequest request,
            @Valid @RequestBody BrainGameSubmitRequest body
    ) {
        Long userId = getUserId(request);
        if (userId == null) return unauthorized();
        log.info("Game result submission from user {}: {}, difficulty={}", userId, body.getGameType(),
                body.getDifficultyLevel());
        return ResponseEntity.ok(brainGameService.submitGameResult(userId, body));
    }

    /**
     * Get full game statistics (all-time + today + weekly).
     *
     * GET /api/v1/brain-games/stats
     */
    @GetMapping("/stats")
    @Operation(summary = "Get game statistics (all-time, today, weekly)")
    public ResponseEntity<?> getUserStats(HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) return unauthorized();
        log.info("Fetching brain game stats for user {}", userId);
        return ResponseEntity.ok(brainGameService.getUserStats(userId));
    }

    /**
     * Get game history (all, or filtered by game type).
     *
     * GET /api/v1/brain-games/history?gameType=NUMBER_SEQUENCE
     */
    @GetMapping("/history")
    @Operation(summary = "Get game history (optionally filter by game type)")
    public ResponseEntity<?> getUserGameHistory(
            HttpServletRequest request,
            @RequestParam(required = false) BrainGameResult.GameType gameType
    ) {
        Long userId = getUserId(request);
        if (userId == null) return unauthorized();
        log.info("Fetching brain game history for user {}", userId);
        List<BrainGameResult> history = (gameType != null)
                ? brainGameService.getUserGameHistoryByType(userId, gameType)
                : brainGameService.getUserGameHistory(userId);
        return ResponseEntity.ok(history);
    }

    /**
     * Get game history for a specific date (Asia/Almaty).
     *
     * GET /api/v1/brain-games/history/date?date=2025-05-10
     */
    @GetMapping("/history/date")
    @Operation(summary = "Get game history for a specific date")
    public ResponseEntity<?> getGameHistoryByDate(
            HttpServletRequest request,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        Long userId = getUserId(request);
        if (userId == null) return unauthorized();

        LocalDate targetDate = date != null ? date : LocalDate.now(ALMATY_ZONE);
        log.info("Fetching brain game history for user {} on date {}", userId, targetDate);

        List<BrainGameResult> history = brainGameService.getUserGameHistoryByDate(userId, targetDate);
        return ResponseEntity.ok(Map.of(
                "date", targetDate.toString(),
                "games", history,
                "count", history.size()
        ));
    }

    /**
     * Get weekly progression status toward next character level.
     *
     * GET /api/v1/brain-games/progression
     */
    @GetMapping("/progression")
    @Operation(summary = "Get weekly game progression status toward level up")
    public ResponseEntity<?> getProgression(HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) return unauthorized();
        log.info("Fetching game progression for user {}", userId);
        return ResponseEntity.ok(gameProgressionService.getProgressionStatus(userId));
    }

    /**
     * Attempt to level up character based on this week's performance.
     * Returns updated CharacterResponse with justLeveledUp flag.
     *
     * POST /api/v1/brain-games/level-up
     */
    @PostMapping("/level-up")
    @Operation(summary = "Attempt character level-up based on weekly game performance")
    public ResponseEntity<?> tryLevelUp(HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) return unauthorized();
        log.info("Level-up attempt by user {}", userId);
        CharacterResponse response = gameProgressionService.tryLevelUp(userId);
        return ResponseEntity.ok(response);
    }

    private ResponseEntity<Map<String, String>> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Unauthorized: missing JWT token"));
    }
}
