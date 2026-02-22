package org.example.nbcheckinservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nbcheckinservice.dto.CheckInRequest;
import org.example.nbcheckinservice.dto.CheckInResponse;
import org.example.nbcheckinservice.dto.CheckInStatsResponse;
import org.example.nbcheckinservice.dto.StreakResponse;
import org.example.nbcheckinservice.service.AnalyticsService;
import org.example.nbcheckinservice.service.DailyCheckInService;
import org.example.nbcheckinservice.service.StreakService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for Daily Check-in operations
 */
@RestController
@RequestMapping("/checkins")
@RequiredArgsConstructor
@Slf4j
public class DailyCheckInController {

    private final DailyCheckInService checkInService;
    private final StreakService streakService;
    private final AnalyticsService analyticsService;

    /**
     * Create a new check-in for today
     * POST /api/v1/checkins
     */
    @PostMapping
    public ResponseEntity<CheckInResponse> createCheckIn(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CheckInRequest request
    ) {
        log.info("POST /checkins - User {} creating check-in", userId);

        CheckInResponse response = checkInService.createCheckIn(userId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get today's check-in
     * GET /api/v1/checkins/today
     */
    @GetMapping("/today")
    public ResponseEntity<CheckInResponse> getTodayCheckIn(
            @AuthenticationPrincipal Long userId
    ) {
        log.info("GET /checkins/today - User {}", userId);

        CheckInResponse response = checkInService.getTodayCheckIn(userId);

        return ResponseEntity.ok(response);
    }

    /**
     * Check if user has checked in today
     * GET /api/v1/checkins/today/exists
     */
    @GetMapping("/today/exists")
    public ResponseEntity<Map<String, Boolean>> hasCheckedInToday(
            @AuthenticationPrincipal Long userId
    ) {
        log.info("GET /checkins/today/exists - User {}", userId);

        boolean exists = checkInService.hasCheckedInToday(userId);

        return ResponseEntity.ok(Map.of(
                "exists", exists,
                "canCheckIn", !exists
        ));
    }

    /**
     * Get check-in for specific date
     * GET /api/v1/checkins/{date}
     */
    @GetMapping("/{date}")
    public ResponseEntity<CheckInResponse> getCheckInByDate(
            @AuthenticationPrincipal Long userId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        log.info("GET /checkins/{} - User {}", date, userId);

        CheckInResponse response = checkInService.getCheckIn(userId, date);

        return ResponseEntity.ok(response);
    }

    /**
     * Update check-in for specific date
     * PUT /api/v1/checkins/{date}
     */
    @PutMapping("/{date}")
    public ResponseEntity<CheckInResponse> updateCheckIn(
            @AuthenticationPrincipal Long userId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Valid @RequestBody CheckInRequest request
    ) {
        log.info("PUT /checkins/{} - User {}", date, userId);

        CheckInResponse response = checkInService.updateCheckIn(userId, date, request);

        return ResponseEntity.ok(response);
    }

    /**
     * Delete check-in for specific date
     * DELETE /api/v1/checkins/{date}
     */
    @DeleteMapping("/{date}")
    public ResponseEntity<Map<String, String>> deleteCheckIn(
            @AuthenticationPrincipal Long userId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        log.info("DELETE /checkins/{} - User {}", date, userId);

        checkInService.deleteCheckIn(userId, date);

        return ResponseEntity.ok(Map.of(
                "message", "Check-in deleted successfully",
                "date", date.toString()
        ));
    }

    /**
     * Get recent check-ins (last 30 days)
     * GET /api/v1/checkins/recent
     */
    @GetMapping("/recent")
    public ResponseEntity<List<CheckInResponse>> getRecentCheckIns(
            @AuthenticationPrincipal Long userId
    ) {
        log.info("GET /checkins/recent - User {}", userId);

        List<CheckInResponse> checkIns = checkInService.getRecentCheckIns(userId);

        return ResponseEntity.ok(checkIns);
    }

    /**
     * Get check-ins within date range
     * GET /api/v1/checkins?startDate=2024-01-01&endDate=2024-01-31
     */
    @GetMapping
    public ResponseEntity<List<CheckInResponse>> getCheckInsInRange(
            @AuthenticationPrincipal Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        log.info("GET /checkins?startDate={}&endDate={} - User {}",
                startDate, endDate, userId);

        List<CheckInResponse> checkIns = checkInService.getCheckInsInRange(
                userId, startDate, endDate
        );

        return ResponseEntity.ok(checkIns);
    }

    /**
     * Mark cognitive game as played for today
     * POST /api/v1/checkins/cognitive-game
     */
    @PostMapping("/cognitive-game")
    public ResponseEntity<Map<String, String>> markCognitiveGamePlayed(
            @AuthenticationPrincipal Long userId
    ) {
        log.info("POST /checkins/cognitive-game - User {}", userId);

        checkInService.markCognitiveGamePlayed(userId, LocalDate.now());

        return ResponseEntity.ok(Map.of(
                "message", "Cognitive game marked as played",
                "date", LocalDate.now().toString()
        ));
    }


    /**
     * Get user's streak information
     * GET /api/v1/checkins/streak
     */
    @GetMapping("/streak")
    public ResponseEntity<StreakResponse> getStreak(
            @AuthenticationPrincipal Long userId
    ) {
        log.info("GET /checkins/streak - User {}", userId);

        StreakResponse response = streakService.getStreakResponse(userId);

        return ResponseEntity.ok(response);
    }

    /**
     * Recalculate user's streak (admin/debug)
     * POST /api/v1/checkins/streak/recalculate
     */
    @PostMapping("/streak/recalculate")
    public ResponseEntity<StreakResponse> recalculateStreak(
            @AuthenticationPrincipal Long userId
    ) {
        log.info("POST /checkins/streak/recalculate - User {}", userId);

        streakService.recalculateStreak(userId);
        StreakResponse response = streakService.getStreakResponse(userId);

        return ResponseEntity.ok(response);
    }


    /**
     * Get weekly statistics (last 7 days)
     * GET /api/v1/checkins/stats/weekly
     */
    @GetMapping("/stats/weekly")
    public ResponseEntity<CheckInStatsResponse> getWeeklyStats(
            @AuthenticationPrincipal Long userId
    ) {
        log.info("GET /checkins/stats/weekly - User {}", userId);

        CheckInStatsResponse stats = analyticsService.getWeeklyStats(userId);

        return ResponseEntity.ok(stats);
    }

    /**
     * Get monthly statistics (last 30 days)
     * GET /api/v1/checkins/stats/monthly
     */
    @GetMapping("/stats/monthly")
    public ResponseEntity<CheckInStatsResponse> getMonthlyStats(
            @AuthenticationPrincipal Long userId
    ) {
        log.info("GET /checkins/stats/monthly - User {}", userId);

        CheckInStatsResponse stats = analyticsService.getMonthlyStats(userId);

        return ResponseEntity.ok(stats);
    }

    /**
     * Get custom statistics for date range
     * GET /api/v1/checkins/stats?startDate=2024-01-01&endDate=2024-01-31
     */
    @GetMapping("/stats")
    public ResponseEntity<CheckInStatsResponse> getCustomStats(
            @AuthenticationPrincipal Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        log.info("GET /checkins/stats?startDate={}&endDate={} - User {}",
                startDate, endDate, userId);

        CheckInStatsResponse stats = analyticsService.getStats(userId, startDate, endDate);

        return ResponseEntity.ok(stats);
    }


    /**
     * Health check endpoint
     * GET /api/v1/checkins/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "daily-checkin-service",
                "timestamp", LocalDate.now().toString()
        ));
    }
}