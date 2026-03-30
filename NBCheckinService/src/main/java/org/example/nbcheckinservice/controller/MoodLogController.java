package org.example.nbcheckinservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nbcheckinservice.dto.MoodLogRequest;
import org.example.nbcheckinservice.dto.MoodLogResponse;
import org.example.nbcheckinservice.service.MoodLogService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for Mood Logs
 * ✅ FIXED: Extract userId from HttpServletRequest
 */
@RestController
@RequestMapping("/mood")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Mood Logs", description = "Mood tracking and analytics")
public class MoodLogController {

    private final MoodLogService moodLogService;

    private Long getUserId(HttpServletRequest request) {
        return (Long) request.getAttribute("userId");
    }

    @PostMapping
    @Operation(summary = "Create a new mood log")
    public ResponseEntity<MoodLogResponse> createMoodLog(
            HttpServletRequest request,
            @Valid @RequestBody MoodLogRequest moodRequest
    ) {
        Long userId = getUserId(request);
        log.info("POST /mood - User {} creating mood log", userId);
        MoodLogResponse response = moodLogService.createMoodLog(userId, moodRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get mood log by ID")
    public ResponseEntity<MoodLogResponse> getMoodLog(
            HttpServletRequest request,
            @PathVariable Long id
    ) {
        Long userId = getUserId(request);
        log.info("GET /mood/{} - User {} fetching mood log", id, userId);
        MoodLogResponse response = moodLogService.getMoodLog(userId, id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Get all mood logs for user")
    public ResponseEntity<List<MoodLogResponse>> getAllMoodLogs(HttpServletRequest request) {
        Long userId = getUserId(request);
        log.info("GET /mood - User {} fetching all mood logs", userId);
        List<MoodLogResponse> logs = moodLogService.getAllMoodLogs(userId);
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/recent")
    @Operation(summary = "Get last 20 mood logs")
    public ResponseEntity<List<MoodLogResponse>> getRecentMoodLogs(HttpServletRequest request) {
        Long userId = getUserId(request);
        log.info("GET /mood/recent - User {} fetching recent mood logs", userId);
        List<MoodLogResponse> logs = moodLogService.getRecentMoodLogs(userId);
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/today")
    @Operation(summary = "Get today's mood logs")
    public ResponseEntity<List<MoodLogResponse>> getTodayMoodLogs(HttpServletRequest request) {
        Long userId = getUserId(request);
        log.info("GET /mood/today - User {} fetching today's mood logs", userId);
        List<MoodLogResponse> logs = moodLogService.getTodayMoodLogs(userId);
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/range")
    @Operation(summary = "Get mood logs in date range")
    public ResponseEntity<List<MoodLogResponse>> getMoodLogsInRange(
            HttpServletRequest request,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime
    ) {
        Long userId = getUserId(request);
        log.info("GET /mood/range - User {} fetching logs from {} to {}", userId, startTime, endTime);
        List<MoodLogResponse> logs = moodLogService.getMoodLogsInRange(userId, startTime, endTime);
        return ResponseEntity.ok(logs);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update mood log")
    public ResponseEntity<MoodLogResponse> updateMoodLog(
            HttpServletRequest request,
            @PathVariable Long id,
            @Valid @RequestBody MoodLogRequest moodRequest
    ) {
        Long userId = getUserId(request);
        log.info("PUT /mood/{} - User {} updating mood log", id, userId);
        MoodLogResponse response = moodLogService.updateMoodLog(userId, id, moodRequest);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete mood log")
    public ResponseEntity<Map<String, String>> deleteMoodLog(
            HttpServletRequest request,
            @PathVariable Long id
    ) {
        Long userId = getUserId(request);
        log.info("DELETE /mood/{} - User {} deleting mood log", id, userId);
        moodLogService.deleteMoodLog(userId, id);
        return ResponseEntity.ok(Map.of(
                "message", "Mood log deleted successfully",
                "id", id.toString()
        ));
    }

    @GetMapping("/average")
    @Operation(summary = "Get average mood in time range")
    public ResponseEntity<Map<String, Object>> getAverageMood(
            HttpServletRequest request,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime
    ) {
        Long userId = getUserId(request);
        log.info("GET /mood/average - User {} getting average mood", userId);
        Double average = moodLogService.getAverageMood(userId, startTime, endTime);
        return ResponseEntity.ok(Map.of(
                "userId", userId,
                "averageMood", average != null ? average : 0.0
        ));
    }

    @GetMapping("/trigger/{trigger}")
    @Operation(summary = "Get mood logs by specific trigger")
    public ResponseEntity<List<MoodLogResponse>> getMoodLogsByTrigger(
            HttpServletRequest request,
            @PathVariable String trigger
    ) {
        Long userId = getUserId(request);
        log.info("GET /mood/trigger/{} - User {} fetching logs", trigger, userId);
        List<MoodLogResponse> logs = moodLogService.getMoodLogsByTrigger(userId, trigger);
        return ResponseEntity.ok(logs);
    }
}