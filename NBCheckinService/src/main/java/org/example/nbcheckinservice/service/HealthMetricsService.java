package org.example.nbcheckinservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nbcheckinservice.dto.HealthMetricsResponse;
import org.example.nbcheckinservice.entity.DailyCheckIn;
import org.example.nbcheckinservice.entity.HealthMetrics;
import org.example.nbcheckinservice.entity.SleepLog;
import org.example.nbcheckinservice.repository.DailyCheckInRepository;
import org.example.nbcheckinservice.repository.HealthMetricsRepository;
import org.example.nbcheckinservice.repository.SleepLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * HealthMetricsService — вычисляет три метрики здоровья на основе реальных данных:
 *   M-Rest    — качество сна и восстановления (из DailyCheckIn + SleepLog)
 *   M-Ready   — когнитивная готовность к дню  (из DailyCheckIn + M-Rest)
 *   M-Balance — эмоциональный баланс          (из DailyCheckIn: стресс, привычки, настроение)
 *
 * Вызывается:
 *   1. Автоматически — через Kafka после создания чекина (async, не блокирует чекин)
 *   2. Вручную       — через GET /health-metrics/today или /health-metrics/recalculate
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HealthMetricsService {

    private final HealthMetricsRepository metricsRepository;
    private final DailyCheckInRepository checkInRepository;
    private final SleepLogRepository sleepLogRepository;

    private static final ZoneId ALMATY_ZONE = ZoneId.of("Asia/Almaty");

    /**
     * Главный метод: вычисляет и сохраняет метрики для userId на указанную дату.
     * Если метрики за эту дату уже есть — обновляет.
     * Если DailyCheckIn за эту дату нет — возвращает Optional.empty().
     */
    @Transactional
    public Optional<HealthMetricsResponse> calculateAndSave(Long userId, LocalDate date) {
        Optional<DailyCheckIn> checkInOpt = checkInRepository.findByUserIdAndCheckInDate(userId, date);
        if (checkInOpt.isEmpty()) {
            log.warn("No check-in found for user {} on {} — skipping health metrics calculation", userId, date);
            return Optional.empty();
        }

        DailyCheckIn checkIn = checkInOpt.get();
        Optional<SleepLog> sleepLogOpt = sleepLogRepository.findByUserIdAndSleepDate(userId, date);

        HealthMetrics metrics = metricsRepository
                .findByUserIdAndMetricDate(userId, date)
                .orElse(HealthMetrics.builder()
                        .userId(userId)
                        .metricDate(date)
                        .build());

        // — Populate fields from DailyCheckIn —
        metrics.setSleepHours(checkIn.getSleepHours() != null
                ? checkIn.getSleepHours().doubleValue() : null);
        metrics.setSleepQuality(checkIn.getSleepQuality());
        metrics.setEnergyLevel(checkIn.getEnergyLevel());
        metrics.setMorningMood(checkIn.getMorningMood());
        metrics.setEveningMood(checkIn.getEveningMood());
        metrics.setStressLevel(checkIn.getStressLevel());
        metrics.setActivityMinutes(checkIn.getPhysicalActivityMinutes());
        metrics.setCognitiveGamesPlayed(checkIn.getCognitiveGameCount());
        metrics.setDidExercise(checkIn.getDidExercise());
        metrics.setAteHealthy(checkIn.getAteHealthy());
        metrics.setHadSocialInteraction(checkIn.getHadSocialInteraction());

        // — Enrich from SleepLog if available —
        sleepLogOpt.ifPresent(sl -> {
            if (sl.getDeepSleepMinutes() != null) metrics.setDeepSleepMinutes(sl.getDeepSleepMinutes());
            if (sl.getRemSleepMinutes() != null) metrics.setRemSleepMinutes(sl.getRemSleepMinutes());
            if (sl.getTotalHours() != null && sl.getTotalHours().doubleValue() > 0) {
                metrics.setTotalSleepMinutes((int) (sl.getTotalHours().doubleValue() * 60));
                // Override sleepHours with more precise value from SleepLog
                metrics.setSleepHours(sl.getTotalHours().doubleValue());
            }
            metrics.setFeltRested(sl.getFeltRested());
            // Override quality with SleepLog's qualityScore if available (1-10)
            if (sl.getQualityScore() != null) {
                metrics.setSleepQuality(sl.getQualityScore());
            }
        });

        // — Run calculation (MRest → MReady → MBalance → Overall) —
        metrics.recalculateAll();

        HealthMetrics saved = metricsRepository.save(metrics);
        log.info("Health metrics saved for user {} on {}: mRest={}, mReady={}, mBalance={}, overall={}",
                userId, date, saved.getMRest(), saved.getMReady(), saved.getMBalance(), saved.getOverallWellnessScore());

        return Optional.of(toResponse(saved));
    }

    /** Метрики за сегодня (Asia/Almaty) */
    @Transactional(readOnly = true)
    public Optional<HealthMetricsResponse> getTodayMetrics(Long userId) {
        return metricsRepository
                .findByUserIdAndMetricDate(userId, LocalDate.now(ALMATY_ZONE))
                .map(this::toResponse);
    }

    /** Метрики за конкретную дату */
    @Transactional(readOnly = true)
    public Optional<HealthMetricsResponse> getMetricsForDate(Long userId, LocalDate date) {
        return metricsRepository
                .findByUserIdAndMetricDate(userId, date)
                .map(this::toResponse);
    }

    /** Последние N дней метрик */
    @Transactional(readOnly = true)
    public List<HealthMetricsResponse> getRecentMetrics(Long userId, int days) {
        LocalDate end = LocalDate.now(ALMATY_ZONE);
        LocalDate start = end.minusDays(days - 1);
        return metricsRepository
                .findByUserIdAndMetricDateBetweenOrderByMetricDateDesc(userId, start, end)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /** Все метрики пользователя (история) */
    @Transactional(readOnly = true)
    public List<HealthMetricsResponse> getAllMetrics(Long userId) {
        return metricsRepository
                .findByUserIdOrderByMetricDateDesc(userId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Принудительный пересчёт метрик за дату (например, если пользователь обновил чекин).
     * Идентичен calculateAndSave, вызывается через отдельный endpoint.
     */
    @Transactional
    public Optional<HealthMetricsResponse> recalculate(Long userId, LocalDate date) {
        log.info("Force-recalculating health metrics for user {} on {}", userId, date);
        return calculateAndSave(userId, date);
    }

    // ========== MAPPER ==========

    private HealthMetricsResponse toResponse(HealthMetrics m) {
        return HealthMetricsResponse.builder()
                .id(m.getId())
                .userId(m.getUserId())
                .metricDate(m.getMetricDate())
                .mRest(m.getMRest())
                .mReady(m.getMReady())
                .mBalance(m.getMBalance())
                .overallWellnessScore(m.getOverallWellnessScore())
                .mRestLabel(m.getMRestLabel())
                .mReadyLabel(m.getMReadyLabel())
                .mBalanceLabel(m.getMBalanceLabel())
                .overallLabel(m.getOverallLabel())
                .sleepHours(m.getSleepHours())
                .sleepQuality(m.getSleepQuality())
                .energyLevel(m.getEnergyLevel())
                .morningMood(m.getMorningMood())
                .eveningMood(m.getEveningMood())
                .stressLevel(m.getStressLevel())
                .activityMinutes(m.getActivityMinutes())
                .cognitiveGamesPlayed(m.getCognitiveGamesPlayed())
                .didExercise(m.getDidExercise())
                .ateHealthy(m.getAteHealthy())
                .hadSocialInteraction(m.getHadSocialInteraction())
                .deepSleepMinutes(m.getDeepSleepMinutes())
                .remSleepMinutes(m.getRemSleepMinutes())
                .feltRested(m.getFeltRested())
                .calculatedAt(m.getUpdatedAt())
                .build();
    }
}
