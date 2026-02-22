package org.example.nbcheckinservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nbcheckinservice.dto.MoodLogRequest;
import org.example.nbcheckinservice.dto.MoodLogResponse;
import org.example.nbcheckinservice.service.MoodLogService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for Mood Logging operations
 * Path: mental-health-service/src/main/java/com/neuralbalance/mentalhealth/controller/MoodLogController.java
 */
@RestController
@RequestMapping("/mood")
@RequiredArgsConstructor
@Slf4j
public class MoodLogController {

    private final MoodLogService moodLogService;

    /**
     * Create a new mood log
     * POST /api/v1/mood
     */
    @PostMapping
    public ResponseEntity<MoodLogResponse> createMoodLog(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody MoodLogRequest request
    ) {
        log.info("POST /mood - User {} creating mood log", userId);

        MoodLogResponse response = moodLogService.createMoodLog(userId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get mood log by ID
     * GET /api/v1/mood/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<MoodLogResponse> getMoodLog(
            @PathVariable Long id
    ) {
        log.info("GET /mood/{} - Fetching mood log", id);

        MoodLogResponse response = moodLogService.getMoodLog(id);

        return ResponseEntity.ok(response);
    }

    /**
     * Get all mood logs for user
     * GET /api/v1/mood
     */
    @GetMapping
    public ResponseEntity<List<MoodLogResponse>> getAllMoodLogs(
            @AuthenticationPrincipal Long userId
    ) {
        log.info("GET /mood - User {} fetching all mood logs", userId);

        List<MoodLogResponse> logs = moodLogService.getAllMoodLogs(userId);

        return ResponseEntity.ok(logs);
    }

    /**
     * Get recent mood logs (last 20)
     * GET /api/v1/mood/recent
     */
    @GetMapping("/recent")
    public ResponseEntity<List<MoodLogResponse>> getRecentMoodLogs(
            @AuthenticationPrincipal Long userId
    ) {
        log.info("GET /mood/recent - User {} fetching recent mood logs", userId);

        List<MoodLogResponse> logs = moodLogService.getRecentMoodLogs(userId);

        return ResponseEntity.ok(logs);
    }

    /**
     * Get today's mood logs
     * GET /api/v1/mood/today
     */
    @GetMapping("/today")
    public ResponseEntity<List<MoodLogResponse>> getTodayMoodLogs(
            @AuthenticationPrincipal Long userId
    ) {
        log.info("GET /mood/today - User {} fetching today's mood logs", userId);

        List<MoodLogResponse> logs = moodLogService.getTodayMoodLogs(userId);

        return ResponseEntity.ok(logs);
    }

    /**
     * Get mood logs within time range
     * GET /api/v1/mood/range?startTime=2024-01-01T00:00:00&endTime=2024-01-31T23:59:59
     */
    @GetMapping("/range")
    public ResponseEntity<List<MoodLogResponse>> getMoodLogsInRange(
            @AuthenticationPrincipal Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime
    ) {
        log.info("GET /mood/range - User {} fetching mood logs from {} to {}",
                userId, startTime, endTime);

        List<MoodLogResponse> logs = moodLogService.getMoodLogsInRange(userId, startTime, endTime);

        return ResponseEntity.ok(logs);
    }

    /**
     * Update mood log
     * PUT /api/v1/mood/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<MoodLogResponse> updateMoodLog(
            @PathVariable Long id,
            @Valid @RequestBody MoodLogRequest request
    ) {
        log.info("PUT /mood/{} - Updating mood log", id);

        MoodLogResponse response = moodLogService.updateMoodLog(id, request);

        return ResponseEntity.ok(response);
    }

    /**
     * Delete mood log
     * DELETE /api/v1/mood/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteMoodLog(
            @PathVariable Long id
    ) {
        log.info("DELETE /mood/{} - Deleting mood log", id);

        moodLogService.deleteMoodLog(id);

        return ResponseEntity.ok(Map.of(
                "message", "Mood log deleted successfully",
                "id", id.toString()
        ));
    }

    /**
     * Get average mood in time range
     * GET /api/v1/mood/average?startTime=...&endTime=...
     */
    @GetMapping("/average")
    public ResponseEntity<Map<String, Object>> getAverageMood(
            @AuthenticationPrincipal Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime
    ) {
        log.info("GET /mood/average - User {} getting average mood from {} to {}",
                userId, startTime, endTime);

        Double average = moodLogService.getAverageMood(userId, startTime, endTime);

        return ResponseEntity.ok(Map.of(
                "userId", userId,
                "startTime", startTime.toString(),
                "endTime", endTime.toString(),
                "averageMood", average
        ));
    }

    /**
     * Get mood logs by trigger
     * GET /api/v1/mood/trigger/{trigger}
     */
    @GetMapping("/trigger/{trigger}")
    public ResponseEntity<List<MoodLogResponse>> getMoodLogsByTrigger(
            @AuthenticationPrincipal Long userId,
            @PathVariable String trigger
    ) {
        log.info("GET /mood/trigger/{} - User {} fetching mood logs by trigger", trigger, userId);

        List<MoodLogResponse> logs = moodLogService.getMoodLogsByTrigger(userId, trigger);

        return ResponseEntity.ok(logs);
    }
}
