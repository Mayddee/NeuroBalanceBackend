package org.example.nbcheckinservice.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nbcheckinservice.dto.SleepLogRequest;
import org.example.nbcheckinservice.dto.SleepLogResponse;
import org.example.nbcheckinservice.service.SleepLogService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for Sleep Logging operations
 * ✅ FIXED: Extract userId from HttpServletRequest
 */
@RestController
@RequestMapping("/sleep")
@RequiredArgsConstructor
@Slf4j
public class SleepLogController {

    private final SleepLogService sleepLogService;

    private Long getUserId(HttpServletRequest request) {
        return (Long) request.getAttribute("userId");
    }

    @PostMapping
    public ResponseEntity<SleepLogResponse> createSleepLog(
            HttpServletRequest request,
            @Valid @RequestBody SleepLogRequest sleepRequest
    ) {
        Long userId = getUserId(request);
        log.info("POST /sleep - User {} creating sleep log", userId);

        SleepLogResponse response = sleepLogService.createSleepLog(userId, sleepRequest);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SleepLogResponse> getSleepLog(
            HttpServletRequest request,
            @PathVariable Long id
    ) {
        Long userId = getUserId(request);
        log.info("GET /sleep/{} - User {} fetching sleep log", id, userId);
        SleepLogResponse response = sleepLogService.getSleepLog(userId, id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<SleepLogResponse>> getAllSleepLogs(HttpServletRequest request) {
        Long userId = getUserId(request);
        log.info("GET /sleep - User {} fetching all sleep logs", userId);

        List<SleepLogResponse> logs = sleepLogService.getAllSleepLogs(userId);

        return ResponseEntity.ok(logs);
    }

    @GetMapping("/recent")
    public ResponseEntity<List<SleepLogResponse>> getRecentSleepLogs(HttpServletRequest request) {
        Long userId = getUserId(request);
        log.info("GET /sleep/recent - User {} fetching recent sleep logs", userId);

        List<SleepLogResponse> logs = sleepLogService.getRecentSleepLogs(userId);

        return ResponseEntity.ok(logs);
    }

    @GetMapping("/today")
    public ResponseEntity<SleepLogResponse> getTodaySleepLog(HttpServletRequest request) {
        Long userId = getUserId(request);
        log.info("GET /sleep/today - User {} fetching today's sleep log", userId);

        SleepLogResponse response = sleepLogService.getTodaySleepLog(userId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/today/exists")
    public ResponseEntity<Map<String, Boolean>> hasSleepLogToday(HttpServletRequest request) {
        Long userId = getUserId(request);
        log.info("GET /sleep/today/exists - User {} checking sleep log existence", userId);

        boolean exists = sleepLogService.hasSleepLogToday(userId);

        return ResponseEntity.ok(Map.of(
                "exists", exists,
                "canLog", !exists
        ));
    }

    @GetMapping("/date/{date}")
    public ResponseEntity<SleepLogResponse> getSleepLogByDate(
            HttpServletRequest request,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        Long userId = getUserId(request);
        log.info("GET /sleep/date/{} - User {} fetching sleep log", date, userId);

        SleepLogResponse response = sleepLogService.getSleepLogByDate(userId, date);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/range")
    public ResponseEntity<List<SleepLogResponse>> getSleepLogsInRange(
            HttpServletRequest request,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        Long userId = getUserId(request);
        log.info("GET /sleep/range - User {} fetching sleep logs from {} to {}",
                userId, startDate, endDate);

        List<SleepLogResponse> logs = sleepLogService.getSleepLogsInRange(
                userId, startDate, endDate
        );

        return ResponseEntity.ok(logs);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SleepLogResponse> updateSleepLog(
            HttpServletRequest request,
            @PathVariable Long id,
            @Valid @RequestBody SleepLogRequest sleepRequest
    ) {
        Long userId = getUserId(request);
        log.info("PUT /sleep/{} - User {} updating sleep log", id, userId);
        SleepLogResponse response = sleepLogService.updateSleepLog(userId, id, sleepRequest);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/date/{date}")
    public ResponseEntity<SleepLogResponse> updateSleepLogByDate(
            HttpServletRequest request,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Valid @RequestBody SleepLogRequest sleepRequest
    ) {
        Long userId = getUserId(request);
        log.info("PUT /sleep/date/{} - User {} updating sleep log", date, userId);

        SleepLogResponse response = sleepLogService.updateSleepLogByDate(
                userId, date, sleepRequest
        );

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteSleepLog(
            HttpServletRequest request,
            @PathVariable Long id
    ) {
        Long userId = getUserId(request);
        log.info("DELETE /sleep/{} - User {} deleting sleep log", id, userId);
        sleepLogService.deleteSleepLog(userId, id);
        return ResponseEntity.ok(Map.of(
                "message", "Sleep log deleted successfully",
                "id", id.toString()
        ));
    }

    @DeleteMapping("/date/{date}")
    public ResponseEntity<Map<String, String>> deleteSleepLogByDate(
            HttpServletRequest request,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        Long userId = getUserId(request);
        log.info("DELETE /sleep/date/{} - User {} deleting sleep log", date, userId);

        sleepLogService.deleteSleepLogByDate(userId, date);

        return ResponseEntity.ok(Map.of(
                "message", "Sleep log deleted successfully",
                "date", date.toString()
        ));
    }
}