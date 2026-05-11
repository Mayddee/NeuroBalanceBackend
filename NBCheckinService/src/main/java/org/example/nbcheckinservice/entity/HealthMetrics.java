package org.example.nbcheckinservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * HealthMetrics — три ключевых показателя здоровья, вычисляемых после каждого чекина:
 *   M-Rest    (0-100) — качество восстановления/сна
 *   M-Ready   (0-100) — когнитивная готовность к дню
 *   M-Balance (0-100) — эмоциональный баланс / управление стрессом
 *
 * Формулы идентичны логике мобильного приложения, но вместо захардкоженных данных
 * используются реальные поля из DailyCheckIn + SleepLog.
 */
@Entity
@Table(name = "health_metrics", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "metric_date"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HealthMetrics {

    private static final ZoneId ALMATY_ZONE = ZoneId.of("Asia/Almaty");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "metric_date", nullable = false)
    private LocalDate metricDate;

    // ========== THREE CORE METRICS (0-100%) ==========

    @Column(name = "m_rest", nullable = false)
    @Builder.Default
    private Integer mRest = 50;

    @Column(name = "m_ready", nullable = false)
    @Builder.Default
    private Integer mReady = 50;

    @Column(name = "m_balance", nullable = false)
    @Builder.Default
    private Integer mBalance = 50;

    @Column(name = "overall_wellness_score", nullable = false)
    @Builder.Default
    private Integer overallWellnessScore = 50;

    // ========== INPUT FROM DailyCheckIn ==========

    /** Sleep hours (0-24) from DailyCheckIn.sleepHours */
    @Column(name = "sleep_hours")
    private Double sleepHours;

    /** Sleep quality (1-10) from DailyCheckIn.sleepQuality */
    @Column(name = "sleep_quality")
    private Integer sleepQuality;

    /** Energy level (1-10) from DailyCheckIn.energyLevel */
    @Column(name = "energy_level")
    private Integer energyLevel;

    /** Morning mood (1-5) from DailyCheckIn.morningMood */
    @Column(name = "morning_mood")
    private Integer morningMood;

    /** Evening mood (1-5) from DailyCheckIn.eveningMood */
    @Column(name = "evening_mood")
    private Integer eveningMood;

    /** Stress level (1-10) from DailyCheckIn.stressLevel */
    @Column(name = "stress_level")
    private Integer stressLevel;

    /** Physical activity minutes from DailyCheckIn.physicalActivityMinutes */
    @Column(name = "activity_minutes")
    private Integer activityMinutes;

    /** Cognitive games played count from DailyCheckIn.cognitiveGameCount */
    @Column(name = "cognitive_games_played")
    private Integer cognitiveGamesPlayed;

    /** From DailyCheckIn.didExercise — used in MBalance */
    @Column(name = "did_exercise")
    private Boolean didExercise;

    /** From DailyCheckIn.ateHealthy — used in MBalance */
    @Column(name = "ate_healthy")
    private Boolean ateHealthy;

    /** From DailyCheckIn.hadSocialInteraction — used in MBalance */
    @Column(name = "had_social_interaction")
    private Boolean hadSocialInteraction;

    // ========== INPUT FROM SleepLog (optional, enriches MRest) ==========

    /** Deep sleep minutes from SleepLog.deepSleepMinutes */
    @Column(name = "deep_sleep_minutes")
    private Integer deepSleepMinutes;

    /** REM sleep minutes from SleepLog.remSleepMinutes */
    @Column(name = "rem_sleep_minutes")
    private Integer remSleepMinutes;

    /** Total sleep minutes (totalHours * 60) from SleepLog */
    @Column(name = "total_sleep_minutes")
    private Integer totalSleepMinutes;

    /** Whether user felt rested from SleepLog.feltRested */
    @Column(name = "felt_rested")
    private Boolean feltRested;

    // ========== METADATA ==========

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now(ALMATY_ZONE);
        updatedAt = LocalDateTime.now(ALMATY_ZONE);
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now(ALMATY_ZONE);
    }

    // ========== CALCULATION METHODS ==========

    /**
     * M-Rest = качество сна и восстановления.
     *
     * Формула (идентично мобильному приложению):
     *   durationScore  = min(sleepHours / 8.0, 1.0) * 50   — длительность сна (макс 50)
     *   qualityScore   — если есть deep+REM из SleepLog:
     *                      min((deep+rem)/total / 0.40, 1.0) * 30
     *                    иначе: (sleepQuality / 10.0) * 30
     *   subjectiveScore— если feltRested != null: 20 (да) / 10 (нет)
     *                    иначе: 20 (нейтральное значение)
     *   Итог: durationScore + qualityScore + subjectiveScore (0-100)
     */
    public void calculateMRest() {
        double hours = sleepHours != null ? sleepHours : 7.0;
        double durationScore = Math.min(hours / 8.0, 1.0) * 50.0;

        double qualityScore;
        if (deepSleepMinutes != null && remSleepMinutes != null
                && totalSleepMinutes != null && totalSleepMinutes > 0) {
            double deepRem = deepSleepMinutes + remSleepMinutes;
            qualityScore = Math.min((deepRem / totalSleepMinutes) / 0.40, 1.0) * 30.0;
        } else if (sleepQuality != null) {
            qualityScore = (sleepQuality / 10.0) * 30.0;
        } else {
            qualityScore = 15.0;
        }

        double subjectiveScore;
        if (feltRested != null) {
            subjectiveScore = feltRested ? 20.0 : 10.0;
        } else {
            subjectiveScore = 20.0;
        }

        mRest = clamp((int) (durationScore + qualityScore + subjectiveScore), 0, 100);
    }

    /**
     * M-Ready = когнитивная готовность к дню.
     *
     * Формула (идентично мобильному приложению):
     *   energyScore = (energyLevel / 10.0) * 40   — уровень энергии (1-10) → 40
     *   moodScore   = (morningMood / 5.0) * 30     — утреннее настроение (1-5) → 30
     *   restScore   = mRest * 0.30                 — вклад качества сна → 30
     *   Итог: energyScore + moodScore + restScore (0-100)
     *
     * ВАЖНО: calculateMRest() должен быть вызван до calculateMReady()
     */
    public void calculateMReady() {
        double energyScore = energyLevel != null ? (energyLevel / 10.0) * 40.0 : 20.0;
        double moodScore = morningMood != null ? (morningMood / 5.0) * 30.0 : 15.0;
        double restScore = mRest * 0.30;

        mReady = clamp((int) (energyScore + moodScore + restScore), 0, 100);
    }

    /**
     * M-Balance = эмоциональный баланс и стрессоустойчивость.
     *
     * Формула (идентично мобильному приложению):
     *   stressScore = max((10 - stressLevel) / 9.0, 0) * 40  — стресс (1-10 инвертированный) → 40
     *   habitsScore = didExercise(13.3) + ateHealthy(13.3) + hadSocialInteraction(13.4) → макс 40
     *   moodScore   = (eveningMood / 5.0) * 20                — вечернее настроение (1-5) → 20
     *   Итог: stressScore + habitsScore + moodScore (0-100)
     */
    public void calculateMBalance() {
        double sl = stressLevel != null ? stressLevel : 5.0;
        double stressScore = Math.max((10.0 - sl) / 9.0, 0) * 40.0;

        double habitsScore = 0.0;
        if (Boolean.TRUE.equals(didExercise)) habitsScore += 13.3;
        if (Boolean.TRUE.equals(ateHealthy)) habitsScore += 13.3;
        if (Boolean.TRUE.equals(hadSocialInteraction)) habitsScore += 13.4;

        double moodScore = eveningMood != null ? (eveningMood / 5.0) * 20.0 : 10.0;

        mBalance = clamp((int) (stressScore + habitsScore + moodScore), 0, 100);
    }

    /** Overall wellness = среднее трёх метрик */
    public void calculateOverallWellness() {
        overallWellnessScore = (mRest + mReady + mBalance) / 3;
    }

    /**
     * Пересчитать все три метрики и итоговый wellness.
     * Порядок важен: MRest → MReady (зависит от mRest) → MBalance → Overall
     */
    public void recalculateAll() {
        calculateMRest();
        calculateMReady();
        calculateMBalance();
        calculateOverallWellness();
    }

    // ========== LABEL HELPERS ==========

    public String getMRestLabel() {
        return scoreLabel(mRest);
    }

    public String getMReadyLabel() {
        return scoreLabel(mReady);
    }

    public String getMBalanceLabel() {
        return scoreLabel(mBalance);
    }

    public String getOverallLabel() {
        return scoreLabel(overallWellnessScore);
    }

    private static String scoreLabel(int score) {
        if (score >= 80) return "Excellent";
        if (score >= 60) return "Good";
        if (score >= 40) return "Fair";
        return "Poor";
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
