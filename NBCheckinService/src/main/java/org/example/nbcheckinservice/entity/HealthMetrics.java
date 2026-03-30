package org.example.nbcheckinservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * HealthMetrics - Calculated health metrics shown on main screen
 * - M-Balance (48%): Emotional balance and stress management capability
 * - M-Rest (76%): Recovery and rest quality
 * - M-Ready (90%): Readiness for cognitive tasks
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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "metric_date", nullable = false)
    private LocalDate metricDate;

    // ========== THREE CORE METRICS ==========

    @Column(name = "m_balance", nullable = false)
    @Builder.Default
    private Integer mBalance = 50; // Emotional balance (0-100%)

    @Column(name = "m_rest", nullable = false)
    @Builder.Default
    private Integer mRest = 50; // Rest quality (0-100%)

    @Column(name = "m_ready", nullable = false)
    @Builder.Default
    private Integer mReady = 50; // Cognitive readiness (0-100%)

    @Column(name = "overall_wellness_score", nullable = false)
    @Builder.Default
    private Integer overallWellnessScore = 63; // Average of all three

    // ========== INPUT DATA FOR CALCULATIONS ==========

    // Stress level (1-5 scale)
    @Column(name = "stress_level")
    private Integer stressLevel;

    // Energy level (0-100% battery)
    @Column(name = "energy_level")
    private Integer energyLevel;

    // Sleep hours
    @Column(name = "sleep_hours")
    private Double sleepHours;

    // Sleep quality (1-5 scale)
    @Column(name = "sleep_quality")
    private Integer sleepQuality;

    // Mood (morning + evening, 1-5 scale)
    @Column(name = "morning_mood")
    private Integer morningMood;

    @Column(name = "evening_mood")
    private Integer eveningMood;

    // Physical activity minutes
    @Column(name = "activity_minutes")
    private Integer activityMinutes;

    // Cognitive games played
    @Column(name = "cognitive_games_played")
    private Integer cognitiveGamesPlayed;

    // Metadata
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Calculate M-Balance (Emotional Balance)
     * Factors: stress level (inverted), mood stability, energy level
     */
    public void calculateMBalance() {
        int score = 50; // Base score

        // Stress (inverted: low stress = high balance)
        if (stressLevel != null) {
            score += (5 - stressLevel) * 5; // Low stress (1) = +20, High stress (5) = 0
        }

        // Mood stability (average of morning/evening)
        if (morningMood != null && eveningMood != null) {
            int avgMood = (morningMood + eveningMood) / 2;
            score += (avgMood - 3) * 10; // Mood 5 = +20, Mood 1 = -20
        }

        // Energy level
        if (energyLevel != null) {
            score += (energyLevel - 50) / 5; // High energy = bonus
        }

        mBalance = Math.max(0, Math.min(100, score)); // Clamp 0-100
    }

    /**
     * Calculate M-Rest (Recovery Quality)
     * Factors: sleep hours, sleep quality, evening mood
     */
    public void calculateMRest() {
        int score = 50; // Base score

        // Sleep hours (optimal: 7-9 hours)
        if (sleepHours != null) {
            if (sleepHours >= 7.0 && sleepHours <= 9.0) {
                score += 25; // Optimal sleep
            } else if (sleepHours >= 6.0 && sleepHours <= 10.0) {
                score += 10; // Acceptable sleep
            } else {
                score -= 10; // Poor sleep duration
            }
        }

        // Sleep quality
        if (sleepQuality != null) {
            score += (sleepQuality - 3) * 10; // Quality 5 = +20, Quality 1 = -20
        }

        // Evening mood (indicator of day recovery)
        if (eveningMood != null) {
            score += (eveningMood - 3) * 5;
        }

        mRest = Math.max(0, Math.min(100, score)); // Clamp 0-100
    }

    /**
     * Calculate M-Ready (Cognitive Readiness)
     * Factors: energy level, morning mood, sleep quality, cognitive games played
     */
    public void calculateMReady() {
        int score = 50; // Base score

        // Energy level (primary factor)
        if (energyLevel != null) {
            score += (energyLevel - 50) / 2; // Energy 100% = +25
        }

        // Morning mood
        if (morningMood != null) {
            score += (morningMood - 3) * 10; // Mood 5 = +20
        }

        // Sleep quality (affects readiness)
        if (sleepQuality != null) {
            score += (sleepQuality - 3) * 5;
        }

        // Cognitive games (shows active brain)
        if (cognitiveGamesPlayed != null && cognitiveGamesPlayed > 0) {
            score += Math.min(10, cognitiveGamesPlayed * 5); // Up to +10
        }

        mReady = Math.max(0, Math.min(100, score)); // Clamp 0-100
    }

    /**
     * Calculate overall wellness score (average of all three)
     */
    public void calculateOverallWellness() {
        overallWellnessScore = (mBalance + mRest + mReady) / 3;
    }

    /**
     * Recalculate all metrics
     */
    public void recalculateAll() {
        calculateMBalance();
        calculateMRest();
        calculateMReady();
        calculateOverallWellness();
    }
}