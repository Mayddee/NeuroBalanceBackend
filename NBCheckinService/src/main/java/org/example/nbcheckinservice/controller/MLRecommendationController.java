package org.example.nbcheckinservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nbcheckinservice.dto.MLMetricsRequest;
import org.example.nbcheckinservice.dto.MLRecommendationResponse;
import org.example.nbcheckinservice.service.MLRecommendationCacheService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ML Recommendations controller.
 *
 * Два пути обновления данных в daily_ml_recommendation (nb_checkin DB):
 *   1. Автоматически — Kafka-события:
 *        check-in создан  → checkin.created  → asyncRefresh() → computeAndCache() → saveOrUpdateDb()
 *        sleep log сохран → sleep.logged     → asyncRefresh() → computeAndCache() → saveOrUpdateDb()
 *        игра завершена   → game.completed   → asyncRefresh() → computeAndCache() → saveOrUpdateDb()
 *   2. Вручную/тест — POST /ml/recommendations/refresh
 *
 * Фронт использует POST /ml/recommendations — принимает тело, возвращает из БД.
 */
@RestController
@RequestMapping("/ml")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "ML Recommendations", description = "Персонализированные рекомендации из реальных данных пользователя")
public class MLRecommendationController {

    private final MLRecommendationCacheService cacheService;

    // ─────────────────────────────────────────────────────────────
    // FRONTEND ENDPOINT — POST /api/v1/ml/recommendations
    // ─────────────────────────────────────────────────────────────

    /**
     * POST /api/v1/ml/recommendations  — основной эндпойнт для фронтенда.
     *
     * Принимает тело MLMetricsRequest (с любыми значениями от фронта) — формат не меняется.
     * Core-параметры из тела ИГНОРИРУЮТСЯ (sleep, stress, exercise, age, gender, reactionTime,
     * memoryTestScore) — они всегда берутся из реальных данных пользователя в БД.
     *
     * Из тела используются только опциональные префы (PATCH-семантика, не сбрасывают старые):
     *   dailyScreenTime, caffeineIntake, dietType
     *
     * Логика ответа (в порядке приоритета):
     *   1. Onboarding обновлён → пересчитать с реальным age/gender, сохранить в БД, вернуть
     *   2. In-memory cache свежий (<30 мин) → вернуть из памяти
     *   3. Есть запись в daily_ml_recommendation на сегодня → вернуть из БД
     *   4. Записи нет (первый запрос) → вычислить из реальных данных, сохранить в БД, вернуть
     *
     * Формат ответа: MLRecommendationResponse (не изменился).
     */
    @PostMapping("/recommendations")
    @Operation(
            summary = "Получить ML-рекомендации (фронтенд POST)",
            description = "Возвращает из daily_ml_recommendation (nb_checkin DB). " +
                    "Core-параметры из тела игнорируются — используются реальные данные. " +
                    "Обновляется автоматически при check-in / sleep / game через Kafka."
    )
    public ResponseEntity<?> getRecommendationsPost(
            HttpServletRequest request,
            @RequestBody(required = false) MLMetricsRequest body
    ) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        String authHeader = request.getHeader("Authorization");
        log.info("POST /ml/recommendations - User {}", userId);

        // Только опциональные префы из тела (core-параметры игнорируем)
        Double  dailyScreenTime = body != null ? body.getDailyScreenTime() : null;
        Integer caffeineIntake  = body != null ? body.getCaffeineIntake()  : null;
        String  dietType        = body != null ? body.getDietType()        : null;

        MLRecommendationResponse recommendations =
                cacheService.getOrCompute(userId, authHeader, dailyScreenTime, caffeineIntake, dietType);

        return ResponseEntity.ok(recommendations);
    }

    // ─────────────────────────────────────────────────────────────
    // HISTORY — доступ к таблице daily_ml_recommendation через Swagger
    // ─────────────────────────────────────────────────────────────

    /**
     * GET /api/v1/ml/recommendations/history
     *
     * Возвращает историю записей таблицы daily_ml_recommendation для текущего пользователя.
     * Показывает дату, когнитивный балл, источник триггера и время последнего обновления.
     *
     * БД: nb_checkin, таблица: daily_ml_recommendation, контейнер: nb-checkin-db
     */
    @GetMapping("/recommendations/history")
    @Operation(
            summary = "История ML-рекомендаций (таблица daily_ml_recommendation)",
            description = "Показывает все записи из daily_ml_recommendation (nb_checkin DB) для пользователя. " +
                    "Триггеры: check-in / sleep / game / mood / onboarding / manual-refresh."
    )
    public ResponseEntity<?> getHistory(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        log.info("GET /ml/recommendations/history - User {}", userId);

        List<Map<String, Object>> history = cacheService.getHistory(userId).stream()
                .map(r -> {
                    Map<String, Object> entry = new java.util.LinkedHashMap<>();
                    entry.put("id", r.getId());
                    entry.put("userId", r.getUserId());
                    entry.put("recommendationDate", r.getRecommendationDate().toString());
                    entry.put("cognitiveScore", r.getCognitiveScore());
                    entry.put("triggerSource", r.getTriggerSource());
                    entry.put("updatedAt", r.getUpdatedAt().toString());
                    entry.put("createdAt", r.getCreatedAt().toString());
                    return entry;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of(
                "userId", userId,
                "totalRecords", history.size(),
                "table", "daily_ml_recommendation",
                "database", "nb_checkin",
                "container", "nb-checkin-db",
                "history", history
        ));
    }

    // ─────────────────────────────────────────────────────────────
    // GET endpoint — тоже работает (для тестирования через браузер/Swagger)
    // ─────────────────────────────────────────────────────────────

    /**
     * GET /api/v1/ml/recommendations — альтернатива для Swagger/тестов.
     * Та же логика что и POST, но принимает опциональные префы как query-params.
     */
    @GetMapping("/recommendations")
    @Operation(
            summary = "Получить ML-рекомендации (GET для тестов/Swagger)",
            description = "Аналог POST /recommendations. Удобен для тестирования через Swagger UI. " +
                    "Фронт использует POST-версию."
    )
    public ResponseEntity<?> getRecommendations(
            HttpServletRequest request,
            @RequestParam(required = false) Double dailyScreenTime,
            @RequestParam(required = false) Integer caffeineIntake,
            @RequestParam(required = false) String dietType
    ) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        String authHeader = request.getHeader("Authorization");
        log.info("GET /ml/recommendations - User {}", userId);

        MLRecommendationResponse recommendations =
                cacheService.getOrCompute(userId, authHeader, dailyScreenTime, caffeineIntake, dietType);

        return ResponseEntity.ok(recommendations);
    }

    // ─────────────────────────────────────────────────────────────
    // INTERNAL REFRESH — НЕ для фронта
    // ─────────────────────────────────────────────────────────────

    /**
     * POST /api/v1/ml/recommendations/refresh — внутренний/тестовый эндпойнт.
     * Форсирует async-пересчёт из реальных данных и сохраняет в daily_ml_recommendation.
     * Kafka делает это автоматически — используй только для тестирования.
     */
    @PostMapping("/recommendations/refresh")
    @Operation(
            summary = "Форс-рефреш ML (внутренний/тест)",
            description = "Запускает async-пересчёт. Сохраняет в DB. НЕ для фронта. " +
                    "Kafka-события делают это автоматически."
    )
    public ResponseEntity<?> triggerRefresh(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        log.info("POST /ml/recommendations/refresh - User {} triggered manual refresh", userId);
        cacheService.asyncRefresh(userId, "manual-refresh");

        return ResponseEntity.ok(Map.of(
                "status", "refresh_triggered",
                "message", "Пересчёт запущен. Через ~1с вызови POST /ml/recommendations.",
                "userId", userId
        ));
    }
}
