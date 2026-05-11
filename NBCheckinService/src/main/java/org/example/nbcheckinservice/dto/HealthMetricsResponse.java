package org.example.nbcheckinservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response DTO для трёх метрик здоровья: M-Rest, M-Ready, M-Balance.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HealthMetricsResponse {

    private Long id;
    private Long userId;
    private LocalDate metricDate;

    // ===== CORE METRICS (0-100) =====
    private Integer mRest;
    private Integer mReady;
    private Integer mBalance;
    private Integer overallWellnessScore;

    // ===== HUMAN-READABLE LABELS =====
    private String mRestLabel;        // "Excellent" / "Good" / "Fair" / "Poor"
    private String mReadyLabel;
    private String mBalanceLabel;
    private String overallLabel;

    // ===== SOURCE DATA (for transparency) =====
    private Double sleepHours;
    private Integer sleepQuality;
    private Integer energyLevel;
    private Integer morningMood;
    private Integer eveningMood;
    private Integer stressLevel;
    private Integer activityMinutes;
    private Integer cognitiveGamesPlayed;
    private Boolean didExercise;
    private Boolean ateHealthy;
    private Boolean hadSocialInteraction;

    // SleepLog enrichment (may be null if no SleepLog for that day)
    private Integer deepSleepMinutes;
    private Integer remSleepMinutes;
    private Boolean feltRested;

    private LocalDateTime calculatedAt;
}
