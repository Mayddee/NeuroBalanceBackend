package org.example.nbcheckinservice.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nbcheckinservice.dto.HealthMetricsResponse;
import org.example.nbcheckinservice.service.HealthMetricsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

/**
 * REST API для метрик здоровья.
 *
 * Все эндпойнты защищены JWT (userId берётся из request.getAttribute("userId"),
 * который устанавливается JwtAuthenticationFilter — идентично остальным контроллерам).
 *
 * Base path (с учётом context-path /api/v1): /api/v1/health-metrics
 */
@RestController
@RequestMapping("/health-metrics")
@RequiredArgsConstructor
@Slf4j
public class HealthMetricsController {

    private final HealthMetricsService healthMetricsService;

    /**
     * GET /api/v1/health-metrics/today
     * Возвращает метрики за сегодня (Asia/Almaty).
     * Метрики вычисляются автоматически после чекина через Kafka.
     */
    @GetMapping("/today")
    public ResponseEntity<?> getTodayMetrics(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized: missing JWT token"));
        }

        log.info("Fetching today health metrics for user {}", userId);
        return healthMetricsService.getTodayMetrics(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/v1/health-metrics/{date}
     * Метрики за конкретную дату (yyyy-MM-dd).
     */
    @GetMapping("/{date}")
    public ResponseEntity<?> getMetricsForDate(
            HttpServletRequest request,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized: missing JWT token"));
        }

        log.info("Fetching health metrics for user {} on {}", userId, date);
        return healthMetricsService.getMetricsForDate(userId, date)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/v1/health-metrics/recent?days=7
     * Последние N дней метрик (по умолчанию 7).
     */
    @GetMapping("/recent")
    public ResponseEntity<?> getRecentMetrics(
            HttpServletRequest request,
            @RequestParam(defaultValue = "7") int days
    ) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized: missing JWT token"));
        }

        if (days < 1 || days > 90) days = 7;
        log.info("Fetching {} days of health metrics for user {}", days, userId);
        return ResponseEntity.ok(healthMetricsService.getRecentMetrics(userId, days));
    }

    /**
     * GET /api/v1/health-metrics/history
     * Полная история метрик.
     */
    @GetMapping("/history")
    public ResponseEntity<?> getAllMetrics(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized: missing JWT token"));
        }

        log.info("Fetching full health metrics history for user {}", userId);
        return ResponseEntity.ok(healthMetricsService.getAllMetrics(userId));
    }

    /**
     * POST /api/v1/health-metrics/recalculate?date=2026-04-14
     * Принудительный пересчёт метрик за указанную дату (или сегодня).
     * Полезно если чекин был обновлён или SleepLog добавлен позже.
     */
    @PostMapping("/recalculate")
    public ResponseEntity<?> recalculate(
            HttpServletRequest request,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized: missing JWT token"));
        }

        LocalDate targetDate = date != null ? date : LocalDate.now(java.time.ZoneId.of("Asia/Almaty"));
        log.info("Manual recalculation of health metrics for user {} on {}", userId, targetDate);

        return healthMetricsService.recalculate(userId, targetDate)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "No check-in found for date: " + targetDate
                                + ". Please create a check-in first.")));
    }
}
