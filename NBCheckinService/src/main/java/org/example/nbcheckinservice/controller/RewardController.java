package org.example.nbcheckinservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nbcheckinservice.dto.RewardResponse;
import org.example.nbcheckinservice.service.RewardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for Rewards/Badges
 * FIXED: Extracts userId from request attribute (set by AuthFilter)
 */
@RestController
@RequestMapping("/rewards")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Rewards", description = "Reward/badge management")
public class RewardController {

    private final RewardService rewardService;

    /**
     * Extract userId from request attribute (set by AuthFilter)
     */
    private Long getUserId(HttpServletRequest request) {
        return (Long) request.getAttribute("userId");
    }

    /**
     * Get all rewards
     * GET /api/v1/rewards
     */
    @GetMapping
    @Operation(summary = "Get all rewards")
    public ResponseEntity<List<RewardResponse>> getAllRewards(HttpServletRequest request) {
        Long userId = getUserId(request);
        log.info("GET /rewards - User {}", userId);

        List<RewardResponse> rewards = rewardService.getAllRewards(userId);
        return ResponseEntity.ok(rewards);
    }

    /**
     * Check and unlock new rewards
     * POST /api/v1/rewards/check
     */
    @PostMapping("/check")
    @Operation(summary = "Check and unlock new rewards")
    public ResponseEntity<List<RewardResponse>> checkAndUnlockRewards(HttpServletRequest request) {
        Long userId = getUserId(request);
        log.info("POST /rewards/check - User {}", userId);

        List<RewardResponse> newRewards = rewardService.checkAndUnlockRewards(userId);
        return ResponseEntity.ok(newRewards);
    }

    /**
     * Get active XP multiplier
     * GET /api/v1/rewards/xp-multiplier
     */
    @GetMapping("/xp-multiplier")
    @Operation(summary = "Get active XP multiplier")
    public ResponseEntity<Map<String, Object>> getActiveXpMultiplier(HttpServletRequest request) {
        Long userId = getUserId(request);
        log.info("GET /rewards/xp-multiplier - User {}", userId);

        double multiplier = rewardService.getActiveXpMultiplier(userId);

        return ResponseEntity.ok(Map.of(
                "userId", userId,
                "xpMultiplier", multiplier,
                "bonusPercentage", (multiplier - 1.0) * 100
        ));
    }
}