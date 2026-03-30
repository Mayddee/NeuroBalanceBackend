package org.example.nbcheckinservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity representing user's earned rewards/badges
 */
@Entity
@Table(name = "user_rewards",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "reward_type"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserReward {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    @NotNull(message = "User ID is required")
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reward_type", nullable = false)
    @NotNull(message = "Reward type is required")
    private RewardType rewardType;

    @Column(name = "is_unlocked", nullable = false)
    @Builder.Default
    private Boolean isUnlocked = false;

    @Column(name = "unlocked_at")
    private LocalDateTime unlockedAt;

    @Column(name = "xp_multiplier")
    @Builder.Default
    private Double xpMultiplier = 1.0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /**
     * Unlock reward
     */
    public void unlock() {
        this.isUnlocked = true;
        this.unlockedAt = LocalDateTime.now();
    }

    // ========== REWARD TYPES ==========

    public enum RewardType {
        STREAK_3_DAYS(
                "⭐ 3 дня подряд",
                "Награда: Ваши XP * 1.1",
                1.1,
                3
        ),
        STREAK_7_DAYS(
                "🏅 7 дней подряд",
                "Награда: Ваши XP * 1.3",
                1.3,
                7
        ),
        STREAK_14_DAYS(
                "🏆 14 дней подряд",
                "Награда: Ваши XP * 1.5",
                1.5,
                14
        ),
        STREAK_21_DAYS(
                "🎯 21 дня подряд",
                "Награда: Ваши XP * 1.8",
                1.8,
                21
        ),
        STREAK_30_DAYS(
                "💎 30 дней подряд",
                "Награда: Ваши XP * 2.0",
                2.0,
                30
        );

        private final String displayName;
        private final String description;
        private final double xpMultiplier;
        private final int requiredStreak;

        RewardType(String displayName, String description, double xpMultiplier, int requiredStreak) {
            this.displayName = displayName;
            this.description = description;
            this.xpMultiplier = xpMultiplier;
            this.requiredStreak = requiredStreak;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }

        public double getXpMultiplier() {
            return xpMultiplier;
        }

        public int getRequiredStreak() {
            return requiredStreak;
        }
    }
}