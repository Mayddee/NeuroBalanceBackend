package org.example.nbcheckinservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nbcheckinservice.dto.StreakResponse;
import org.example.nbcheckinservice.entity.UserStreak;
import org.example.nbcheckinservice.service.StreakService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/streaks")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Streaks", description = "Check-in streak tracking, milestones, and leaderboard")
public class StreakController {

    private final StreakService streakService;

    private Long getUserId(HttpServletRequest request) {
        return (Long) request.getAttribute("userId");
    }

    @GetMapping
    @Operation(summary = "Get current streak info for the authenticated user",
               description = "Returns currentStreak, longestStreak, totalCheckins, totalXpEarned, nextMilestone, canCheckinToday")
    public ResponseEntity<StreakResponse> getMyStreak(HttpServletRequest request) {
        Long userId = getUserId(request);
        log.info("GET /streaks - User {}", userId);
        return ResponseEntity.ok(streakService.getStreakResponse(userId));
    }

    @PostMapping("/recalculate")
    @Operation(summary = "Recalculate streak from check-in history",
               description = "Rebuilds currentStreak and longestStreak by scanning all past check-ins. Use if streak looks incorrect.")
    public ResponseEntity<Map<String, Object>> recalculate(HttpServletRequest request) {
        Long userId = getUserId(request);
        log.info("POST /streaks/recalculate - User {}", userId);
        UserStreak streak = streakService.recalculateStreak(userId);
        return ResponseEntity.ok(Map.of(
                "userId", userId,
                "currentStreak", streak.getCurrentStreak(),
                "longestStreak", streak.getLongestStreak(),
                "totalCheckins", streak.getTotalCheckins()
        ));
    }

    @GetMapping("/leaderboard/streak")
    @Operation(summary = "Top 10 users by current streak")
    public ResponseEntity<List<UserStreak>> topByStreak() {
        return ResponseEntity.ok(streakService.getTopStreaks(10));
    }

    @GetMapping("/leaderboard/xp")
    @Operation(summary = "Top 10 users by total XP earned from streaks")
    public ResponseEntity<List<UserStreak>> topByXp() {
        return ResponseEntity.ok(streakService.getTopXP(10));
    }

    @GetMapping("/rank")
    @Operation(summary = "Get current user's rank by streak and XP")
    public ResponseEntity<Map<String, Object>> getMyRank(HttpServletRequest request) {
        Long userId = getUserId(request);
        log.info("GET /streaks/rank - User {}", userId);
        Long streakRank = streakService.getUserRankByStreak(userId);
        Long xpRank = streakService.getUserRankByXP(userId);
        return ResponseEntity.ok(Map.of(
                "userId", userId,
                "streakRank", streakRank != null ? streakRank : 0,
                "xpRank", xpRank != null ? xpRank : 0
        ));
    }
}