package org.example.nbcheckinservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nbcheckinservice.dto.SleepLogRequest;
import org.example.nbcheckinservice.dto.SleepLogResponse;
import org.example.nbcheckinservice.service.SleepLogService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for Sleep Logging operations
 * Path: mental-health-service/src/main/java/com/neuralbalance/mentalhealth/controller/SleepLogController.java
 *
 * All endpoints:
 * POST   /api/v1/sleep                    - Create sleep log
 * GET    /api/v1/sleep/{id}               - Get sleep log by ID
 * GET    /api/v1/sleep                    - Get all sleep logs
 * GET    /api/v1/sleep/recent             - Get recent sleep logs (30 days)
 * GET    /api/v1/sleep/today              - Get today's sleep log
 * GET    /api/v1/sleep/today/exists       - Check if sleep log exists today
 * GET    /api/v1/sleep/date/{date}        - Get sleep log by date
 * GET    /api/v1/sleep/range              - Get sleep logs in date range
 * PUT    /api/v1/sleep/{id}               - Update sleep log by ID
 * PUT    /api/v1/sleep/date/{date}        - Update sleep log by date
 * DELETE /api/v1/sleep/{id}               - Delete sleep log by ID
 * DELETE /api/v1/sleep/date/{date}        - Delete sleep log by date
 */
@RestController
@RequestMapping("/sleep")
@RequiredArgsConstructor
@Slf4j
public class SleepLogController {

    private final SleepLogService sleepLogService;

    /**
     * Create a new sleep log
     * POST /api/v1/sleep
     */
    @PostMapping
    public ResponseEntity<SleepLogResponse> createSleepLog(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody SleepLogRequest request
    ) {
        log.info("POST /sleep - User {} creating sleep log", userId);

        SleepLogResponse response = sleepLogService.createSleepLog(userId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get sleep log by ID
     * GET /api/v1/sleep/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<SleepLogResponse> getSleepLog(
            @PathVariable Long id
    ) {
        log.info("GET /sleep/{} - Fetching sleep log", id);

        SleepLogResponse response = sleepLogService.getSleepLog(id);

        return ResponseEntity.ok(response);
    }

    /**
     * Get all sleep logs for user
     * GET /api/v1/sleep
     */
    @GetMapping
    public ResponseEntity<List<SleepLogResponse>> getAllSleepLogs(
            @AuthenticationPrincipal Long userId
    ) {
        log.info("GET /sleep - User {} fetching all sleep logs", userId);

        List<SleepLogResponse> logs = sleepLogService.getAllSleepLogs(userId);

        return ResponseEntity.ok(logs);
    }

    /**
     * Get recent sleep logs (last 30 days)
     * GET /api/v1/sleep/recent
     */
    @GetMapping("/recent")
    public ResponseEntity<List<SleepLogResponse>> getRecentSleepLogs(
            @AuthenticationPrincipal Long userId
    ) {
        log.info("GET /sleep/recent - User {} fetching recent sleep logs", userId);

        List<SleepLogResponse> logs = sleepLogService.getRecentSleepLogs(userId);

        return ResponseEntity.ok(logs);
    }

    /**
     * Get today's sleep log
     * GET /api/v1/sleep/today
     */
    @GetMapping("/today")
    public ResponseEntity<SleepLogResponse> getTodaySleepLog(
            @AuthenticationPrincipal Long userId
    ) {
        log.info("GET /sleep/today - User {} fetching today's sleep log", userId);

        SleepLogResponse response = sleepLogService.getTodaySleepLog(userId);

        return ResponseEntity.ok(response);
    }

    /**
     * Check if sleep log exists for today
     * GET /api/v1/sleep/today/exists
     */
    @GetMapping("/today/exists")
    public ResponseEntity<Map<String, Boolean>> hasSleepLogToday(
            @AuthenticationPrincipal Long userId
    ) {
        log.info("GET /sleep/today/exists - User {} checking sleep log existence", userId);

        boolean exists = sleepLogService.hasSleepLogToday(userId);

        return ResponseEntity.ok(Map.of(
                "exists", exists,
                "canLog", !exists
        ));
    }

    /**
     * Get sleep log by date
     * GET /api/v1/sleep/date/{date}
     */
    @GetMapping("/date/{date}")
    public ResponseEntity<SleepLogResponse> getSleepLogByDate(
            @AuthenticationPrincipal Long userId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        log.info("GET /sleep/date/{} - User {} fetching sleep log", date, userId);

        SleepLogResponse response = sleepLogService.getSleepLogByDate(userId, date);

        return ResponseEntity.ok(response);
    }

    /**
     * Get sleep logs within date range
     * GET /api/v1/sleep/range?startDate=2024-01-01&endDate=2024-01-31
     */
    @GetMapping("/range")
    public ResponseEntity<List<SleepLogResponse>> getSleepLogsInRange(
            @AuthenticationPrincipal Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        log.info("GET /sleep/range - User {} fetching sleep logs from {} to {}",
                userId, startDate, endDate);

        List<SleepLogResponse> logs = sleepLogService.getSleepLogsInRange(
                userId, startDate, endDate
        );

        return ResponseEntity.ok(logs);
    }

    /**
     * Update sleep log by ID
     * PUT /api/v1/sleep/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<SleepLogResponse> updateSleepLog(
            @PathVariable Long id,
            @Valid @RequestBody SleepLogRequest request
    ) {
        log.info("PUT /sleep/{} - Updating sleep log", id);

        SleepLogResponse response = sleepLogService.updateSleepLog(id, request);

        return ResponseEntity.ok(response);
    }

    /**
     * Update sleep log by date
     * PUT /api/v1/sleep/date/{date}
     */
    @PutMapping("/date/{date}")
    public ResponseEntity<SleepLogResponse> updateSleepLogByDate(
            @AuthenticationPrincipal Long userId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Valid @RequestBody SleepLogRequest request
    ) {
        log.info("PUT /sleep/date/{} - User {} updating sleep log", date, userId);

        SleepLogResponse response = sleepLogService.updateSleepLogByDate(
                userId, date, request
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Delete sleep log by ID
     * DELETE /api/v1/sleep/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteSleepLog(
            @PathVariable Long id
    ) {
        log.info("DELETE /sleep/{} - Deleting sleep log", id);

        sleepLogService.deleteSleepLog(id);

        return ResponseEntity.ok(Map.of(
                "message", "Sleep log deleted successfully",
                "id", id.toString()
        ));
    }

    /**
     * Delete sleep log by date
     * DELETE /api/v1/sleep/date/{date}
     */
    @DeleteMapping("/date/{date}")
    public ResponseEntity<Map<String, String>> deleteSleepLogByDate(
            @AuthenticationPrincipal Long userId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        log.info("DELETE /sleep/date/{} - User {} deleting sleep log", date, userId);

        sleepLogService.deleteSleepLogByDate(userId, date);

        return ResponseEntity.ok(Map.of(
                "message", "Sleep log deleted successfully",
                "date", date.toString()
        ));
    }
}
