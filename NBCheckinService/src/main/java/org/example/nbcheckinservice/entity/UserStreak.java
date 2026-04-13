package org.example.nbcheckinservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Table(name = "user_streaks")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class UserStreak {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "current_streak", nullable = false)
    @Builder.Default
    private Integer currentStreak = 0;

    @Column(name = "longest_streak", nullable = false)
    @Builder.Default
    private Integer longestStreak = 0;

    @Column(name = "last_checkin_date")
    private LocalDate lastCheckinDate;

    @Column(name = "total_checkins", nullable = false)
    @Builder.Default
    private Integer totalCheckins = 0;

    @Column(name = "total_xp_earned", nullable = false)
    @Builder.Default
    private Integer totalXpEarned = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Константа для времени
    private static final ZoneId ALMATY_ZONE = ZoneId.of("Asia/Almaty");

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now(ALMATY_ZONE);
        updatedAt = LocalDateTime.now(ALMATY_ZONE);
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now(ALMATY_ZONE);
    }

    public Integer calculateStreakBonusXP() {
        return switch (currentStreak) {
            case 7 -> 50;
            case 14 -> 100;
            case 30 -> 300;
            case 100 -> 1000;
            default -> 0;
        };
    }

    public boolean isMilestoneDay() {
        return currentStreak == 7 || currentStreak == 14 || currentStreak == 30 || currentStreak == 100;
    }

    public String getStreakStatusMessage() {
        if (currentStreak == 0) return "Start your streak today! 🚀";
        if (currentStreak == 1) return "Great start! Keep it up! 💪";
        return String.format("You're on a %d-day streak! 🔥", currentStreak);
    }

    public String getNextMilestone() {
        if (currentStreak < 7) return String.format("%d days until 7-day milestone", 7 - currentStreak);
        if (currentStreak < 14) return String.format("%d days until 14-day milestone", 14 - currentStreak);
        if (currentStreak < 30) return String.format("%d days until 30-day milestone", 30 - currentStreak);
        return "Keep going! 🎉";
    }
}