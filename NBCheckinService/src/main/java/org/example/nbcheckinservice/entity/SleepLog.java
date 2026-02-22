package org.example.nbcheckinservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity representing a detailed sleep log
 * Path: mental-health-service/src/main/java/com/neuralbalance/mentalhealth/entity/SleepLog.java
 */
@Entity
@Table(name = "sleep_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SleepLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    @NotNull(message = "User ID is required")
    private Long userId;

    @Column(name = "sleep_date", nullable = false)
    @NotNull(message = "Sleep date is required")
    private LocalDate sleepDate;

    // ========== SLEEP TIMING ==========

    @Column(name = "bedtime")
    private LocalDateTime bedtime;

    @Column(name = "wake_time")
    private LocalDateTime wakeTime;

    @Column(name = "fell_asleep_time")
    private LocalDateTime fellAsleepTime;

    // ========== DURATION ==========

    @Column(name = "total_hours", precision = 4, scale = 2)
    @DecimalMin(value = "0.0", message = "Total hours cannot be negative")
    @DecimalMax(value = "24.0", message = "Total hours cannot exceed 24")
    private BigDecimal totalHours;

    @Column(name = "actual_sleep_hours", precision = 4, scale = 2)
    @DecimalMin(value = "0.0", message = "Actual sleep hours cannot be negative")
    private BigDecimal actualSleepHours;

    @Column(name = "time_to_fall_asleep_minutes")
    @Min(value = 0, message = "Time to fall asleep cannot be negative")
    private Integer timeToFallAsleepMinutes;

    // ========== QUALITY ==========

    @Column(name = "quality_score")
    @Min(value = 1, message = "Quality score must be between 1 and 10")
    @Max(value = 10, message = "Quality score must be between 1 and 10")
    private Integer qualityScore;

    @Column(name = "sleep_efficiency", precision = 5, scale = 2)
    private BigDecimal sleepEfficiency; // percentage

    @Column(name = "felt_rested")
    private Boolean feltRested;

    // ========== INTERRUPTIONS ==========

    @Column(name = "interruptions_count")
    @Builder.Default
    private Integer interruptionsCount = 0;

    @Column(name = "awake_duration_minutes")
    @Builder.Default
    private Integer awakeDurationMinutes = 0;

    @Column(name = "bathroom_trips")
    @Builder.Default
    private Integer bathroomTrips = 0;

    // ========== SLEEP STAGES ==========

    @Column(name = "deep_sleep_minutes")
    private Integer deepSleepMinutes;

    @Column(name = "light_sleep_minutes")
    private Integer lightSleepMinutes;

    @Column(name = "rem_sleep_minutes")
    private Integer remSleepMinutes;

    @Column(name = "awake_minutes")
    private Integer awakeMinutes;

    // ========== DREAMS ==========

    @Column(name = "had_dreams")
    private Boolean hadDreams;

    @Column(name = "dream_recall", length = 20)
    private String dreamRecall; // none, vague, clear, vivid

    @Column(name = "dream_notes", columnDefinition = "TEXT")
    private String dreamNotes;

    @Column(name = "nightmares")
    @Builder.Default
    private Boolean nightmares = false;

    // ========== ENVIRONMENT ==========

    @Column(name = "room_temperature", length = 20)
    private String roomTemperature; // cold, comfortable, warm, hot

    @Column(name = "noise_level", length = 20)
    private String noiseLevel; // silent, quiet, moderate, noisy

    @Column(name = "light_level", length = 20)
    private String lightLevel; // dark, dim, bright

    @Column(name = "bed_comfort", length = 20)
    private String bedComfort; // uncomfortable, okay, comfortable, very_comfortable

    // ========== PRE-SLEEP FACTORS ==========

    @Column(name = "caffeine_before_bed")
    private Boolean caffeineBeforeBed;

    @Column(name = "screen_time_before_bed_minutes")
    private Integer screenTimeBeforeBedMinutes;

    @Column(name = "exercise_before_bed")
    private Boolean exerciseBeforeBed;

    @Column(name = "alcohol")
    private Boolean alcohol;

    @Column(name = "heavy_meal")
    private Boolean heavyMeal;

    // ========== MORNING FEELING ==========

    @Column(name = "morning_mood")
    @Min(value = 1, message = "Morning mood must be between 1 and 5")
    @Max(value = 5, message = "Morning mood must be between 1 and 5")
    private Integer morningMood;

    @Column(name = "morning_energy")
    @Min(value = 1, message = "Morning energy must be between 1 and 10")
    @Max(value = 10, message = "Morning energy must be between 1 and 10")
    private Integer morningEnergy;

    // ========== NOTES ==========

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // ========== METADATA ==========

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();

        // Calculate sleep efficiency if not set
        if (sleepEfficiency == null && actualSleepHours != null && totalHours != null
                && totalHours.compareTo(BigDecimal.ZERO) > 0) {
            sleepEfficiency = actualSleepHours
                    .multiply(BigDecimal.valueOf(100))
                    .divide(totalHours, 2, BigDecimal.ROUND_HALF_UP);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();

        // Recalculate sleep efficiency
        if (actualSleepHours != null && totalHours != null
                && totalHours.compareTo(BigDecimal.ZERO) > 0) {
            sleepEfficiency = actualSleepHours
                    .multiply(BigDecimal.valueOf(100))
                    .divide(totalHours, 2, BigDecimal.ROUND_HALF_UP);
        }
    }

    /**
     * Calculate overall sleep score (0-100)
     */
    public Double calculateSleepScore() {
        double score = 0.0;
        int factors = 0;

        // Quality score contribution (40%)
        if (qualityScore != null) {
            score += (qualityScore / 10.0) * 40;
            factors++;
        }

        // Duration contribution (30%)
        if (actualSleepHours != null) {
            double hours = actualSleepHours.doubleValue();
            double durationScore = 0.0;

            if (hours >= 7 && hours <= 9) {
                durationScore = 1.0; // Optimal
            } else if (hours >= 6 && hours < 7) {
                durationScore = 0.8;
            } else if (hours >= 9 && hours <= 10) {
                durationScore = 0.8;
            } else if (hours >= 5 && hours < 6) {
                durationScore = 0.5;
            } else {
                durationScore = 0.3;
            }

            score += durationScore * 30;
            factors++;
        }

        // Efficiency contribution (20%)
        if (sleepEfficiency != null) {
            score += (sleepEfficiency.doubleValue() / 100.0) * 20;
            factors++;
        }

        // Felt rested contribution (10%)
        if (feltRested != null) {
            score += (feltRested ? 10 : 0);
            factors++;
        }

        return factors > 0 ? Math.min(score, 100.0) : 0.0;
    }
}
