package org.example.nbcheckinservice.entity;


import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

/**
 * Entity representing a daily check-in record
 */
@Entity
@Table(
        name = "daily_check_ins",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "check_in_date"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyCheckIn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    @NotNull(message = "User ID is required")
    private Long userId;

    @Column(name = "check_in_date", nullable = false)
    @NotNull(message = "Check-in date is required")
    private LocalDate checkInDate;

    // ========== MOOD ==========

    @Column(name = "morning_mood")
    @Min(value = 1, message = "Morning mood must be between 1 and 5")
    @Max(value = 5, message = "Morning mood must be between 1 and 5")
    private Integer morningMood;

    @Column(name = "evening_mood")
    @Min(value = 1, message = "Evening mood must be between 1 and 5")
    @Max(value = 5, message = "Evening mood must be between 1 and 5")
    private Integer eveningMood;

    @Column(name = "morning_mood_emoji", length = 10)
    private String morningMoodEmoji;

    @Column(name = "evening_mood_emoji", length = 10)
    private String eveningMoodEmoji;

    // ========== SLEEP ==========

    @Column(name = "sleep_quality")
    @Min(value = 1, message = "Sleep quality must be between 1 and 10")
    @Max(value = 10, message = "Sleep quality must be between 1 and 10")
    private Integer sleepQuality;

    @Column(name = "sleep_hours", precision = 3, scale = 1)
    @DecimalMin(value = "0.0", message = "Sleep hours cannot be negative")
    @DecimalMax(value = "24.0", message = "Sleep hours cannot exceed 24")
    private BigDecimal sleepHours;

    @Column(name = "sleep_bedtime")
    private LocalTime sleepBedtime;

    @Column(name = "sleep_waketime")
    private LocalTime sleepWaketime;

    // ========== ENERGY & STRESS ==========

    @Column(name = "energy_level")
    @Min(value = 1, message = "Energy level must be between 1 and 10")
    @Max(value = 10, message = "Energy level must be between 1 and 10")
    private Integer energyLevel;

    @Column(name = "stress_level")
    @Min(value = 1, message = "Stress level must be between 1 and 10")
    @Max(value = 10, message = "Stress level must be between 1 and 10")
    private Integer stressLevel;

    // ========== PHYSICAL ACTIVITY ==========

    @Column(name = "physical_activity_minutes")
    @Min(value = 0, message = "Activity minutes cannot be negative")
    @Builder.Default
    private Integer physicalActivityMinutes = 0;

    @Column(name = "physical_activity_type", length = 50)
    private String physicalActivityType; // walk, gym, yoga, sports, none

    // ========== QUICK QUESTIONS (Yes/No) ==========

    @Column(name = "did_exercise")
    @Builder.Default
    private Boolean didExercise = false;

    @Column(name = "ate_healthy")
    @Builder.Default
    private Boolean ateHealthy = false;

    @Column(name = "had_social_interaction")
    @Builder.Default
    private Boolean hadSocialInteraction = false;

    // ========== COGNITIVE GAMES ==========

    @Column(name = "played_cognitive_game_today")
    @Builder.Default
    private Boolean playedCognitiveGameToday = false;

    @Column(name = "cognitive_game_count")
    @Builder.Default
    private Integer cognitiveGameCount = 0;

    // ========== METADATA ==========

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();

        // Set default date to today if not provided
        if (checkInDate == null) {
            checkInDate = LocalDate.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Calculate overall wellness score (0-100)
     * Based on all available metrics
     */
    public Double calculateWellnessScore() {
        double score = 0.0;
        int factorsCount = 0;

        // Mood contribution (0-25 points)
        if (morningMood != null && eveningMood != null) {
            double avgMood = (morningMood + eveningMood) / 2.0;
            score += (avgMood / 5.0) * 25;
            factorsCount++;
        }

        // Sleep contribution (0-25 points)
        if (sleepQuality != null) {
            score += (sleepQuality / 10.0) * 25;
            factorsCount++;
        }

        // Energy contribution (0-25 points)
        if (energyLevel != null) {
            score += (energyLevel / 10.0) * 25;
            factorsCount++;
        }

        // Stress contribution (0-25 points, inverted)
        if (stressLevel != null) {
            score += ((10 - stressLevel) / 10.0) * 25;
            factorsCount++;
        }

        // Bonus points for positive behaviors
        if (Boolean.TRUE.equals(didExercise)) score += 5;
        if (Boolean.TRUE.equals(ateHealthy)) score += 5;
        if (Boolean.TRUE.equals(hadSocialInteraction)) score += 5;
        if (Boolean.TRUE.equals(playedCognitiveGameToday)) score += 5;

        return factorsCount > 0 ? Math.min(score, 100.0) : 0.0;
    }

    /**
     * Get mood emoji based on value
     */
    public static String getMoodEmoji(Integer mood) {
        if (mood == null) return "😐";
        return switch (mood) {
            case 1 -> "😡"; // Very bad
            case 2 -> "😰"; // Bad
            case 3 -> "😢"; // Neutral-sad
            case 4 -> "😐"; // Neutral
            case 5 -> "😊"; // Good
            default -> "😐";
        };
    }

    /**
     * Check if check-in is complete (all required fields filled)
     */
    public boolean isComplete() {
        return morningMood != null
                && sleepQuality != null
                && energyLevel != null
                && stressLevel != null;
    }
}
