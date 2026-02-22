package org.example.nbcheckinservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity tracking user's check-in streaks and XP
 */
@Entity
@Table(name = "user_streaks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserStreak {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    @NotNull(message = "User ID is required")
    private Long userId;

    // ========== STREAK COUNTERS ==========

    @Column(name = "current_streak", nullable = false)
    @Min(value = 0, message = "Current streak cannot be negative")
    @Builder.Default
    private Integer currentStreak = 0;

    @Column(name = "longest_streak", nullable = false)
    @Min(value = 0, message = "Longest streak cannot be negative")
    @Builder.Default
    private Integer longestStreak = 0;

    // ========== ACTIVITY TRACKING ==========

    @Column(name = "last_checkin_date")
    private LocalDate lastCheckinDate;

    @Column(name = "total_checkins", nullable = false)
    @Min(value = 0, message = "Total check-ins cannot be negative")
    @Builder.Default
    private Integer totalCheckins = 0;

    // ========== XP TRACKING ==========

    @Column(name = "total_xp_earned", nullable = false)
    @Min(value = 0, message = "Total XP cannot be negative")
    @Builder.Default
    private Integer totalXpEarned = 0;

    // ========== METADATA ==========

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
     * Calculate streak bonus XP based on current streak
     * Milestones: 7 days, 14 days, 30 days, 100 days
     */
    public Integer calculateStreakBonusXP() {
        return switch (currentStreak) {
            case 7 -> 50;   // Week streak
            case 14 -> 100;  // Two weeks
            case 30 -> 300;  // Month streak
            case 100 -> 1000; // 100 days!
            default -> 0;
        };
    }

    /**
     * Check if user is eligible for milestone bonus
     */
    public boolean isMilestoneDay() {
        return currentStreak == 7
                || currentStreak == 14
                || currentStreak == 30
                || currentStreak == 100;
    }

    /**
     * Get streak status message
     */
    public String getStreakStatusMessage() {
        if (currentStreak == 0) {
            return "Start your streak today! 🚀";
        } else if (currentStreak == 1) {
            return "Great start! Keep it up! 💪";
        } else if (currentStreak < 7) {
            return String.format("You're on a %d-day streak! 🔥", currentStreak);
        } else if (currentStreak < 30) {
            return String.format("Amazing %d-day streak! 🌟", currentStreak);
        } else {
            return String.format("Legendary %d-day streak! 🏆", currentStreak);
        }
    }

    /**
     * Get next milestone info
     */
    public String getNextMilestone() {
        if (currentStreak < 7) {
            return String.format("%d days until 7-day milestone (+50 XP)", 7 - currentStreak);
        } else if (currentStreak < 14) {
            return String.format("%d days until 14-day milestone (+100 XP)", 14 - currentStreak);
        } else if (currentStreak < 30) {
            return String.format("%d days until 30-day milestone (+300 XP)", 30 - currentStreak);
        } else if (currentStreak < 100) {
            return String.format("%d days until 100-day milestone (+1000 XP)", 100 - currentStreak);
        } else {
            return "You've reached all milestones! Keep going! 🎉";
        }
    }
}
