package org.example.nbcheckinservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.ZoneId;

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

    private static final ZoneId ALMATY_ZONE = ZoneId.of("Asia/Almaty");

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now(ALMATY_ZONE);
    }

    /**
     * Unlock reward
     */
    public void unlock() {
        this.isUnlocked = true;
        this.unlockedAt = LocalDateTime.now(ALMATY_ZONE);
    }

    // ========== REWARD TYPES ==========

    public enum RewardType {
        // ===== STREAK MILESTONES (requiredStreak > 0) =====
        STREAK_3_DAYS("⭐ 3 дня подряд", "Чекин 3 дня подряд — XP ×1.1", 1.1, 3),
        STREAK_7_DAYS("🏅 Неделя подряд", "Чекин 7 дней подряд — XP ×1.3", 1.3, 7),
        STREAK_14_DAYS("🏆 Две недели подряд", "Чекин 14 дней подряд — XP ×1.5", 1.5, 14),
        STREAK_20_DAYS("🔥 20 дней подряд", "Чекин 20 дней подряд — XP ×1.7", 1.7, 20),
        STREAK_21_DAYS("🎯 21 день подряд", "Чекин 21 день подряд — XP ×1.8", 1.8, 21),
        STREAK_30_DAYS("💎 Месяц подряд", "Чекин 30 дней подряд — XP ×2.0", 2.0, 30),
        STREAK_50_DAYS("👑 50 дней подряд", "Невероятный стрик! XP ×2.5", 2.5, 50),
        STREAK_100_DAYS("🌟 100 дней подряд", "Легенда! XP ×3.0", 3.0, 100),

        // ===== ACHIEVEMENT BADGES (requiredStreak = 0, unlocked by activity) =====
        FIRST_GAME_PLAYED("🎮 Первая игра", "Ты сыграл в первую игру — начало большого пути!", 1.0, 0),
        GAME_MASTER_10("🎯 Геймер", "Сыграл 10 игр — держись курса!", 1.05, 0),
        GAME_MASTER_50("🏅 Мастер игр", "50 игр пройдено — ты настоящий мастер!", 1.1, 0),
        FIRST_MOOD_LOG("😊 Трекер эмоций", "Записал первое настроение — осознанность растёт!", 1.0, 0),
        MOOD_TRACKER_7("📊 Эмоциональный аналитик", "7 записей настроения — понимаешь себя!", 1.05, 0),
        FIRST_LEVEL_UP("⬆️ Первый уровень", "Персонаж достиг 2 уровня — прогресс виден!", 1.0, 0),
        LEVEL_3_REACHED("🌟 Уровень 3", "Персонаж достиг 3 уровня — XP ×1.1 за игры!", 1.1, 0),
        PERFECT_DAY("✅ Идеальный день", "Выполнил все 5 задач за один день — браво!", 1.1, 0);

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

        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
        public double getXpMultiplier() { return xpMultiplier; }
        public int getRequiredStreak() { return requiredStreak; }
        public boolean isStreakBased() { return requiredStreak > 0; }
    }
}