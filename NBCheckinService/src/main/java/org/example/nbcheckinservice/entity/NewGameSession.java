package org.example.nbcheckinservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Table(name = "new_game_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewGameSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "game_type", nullable = false)
    private GameType gameType;

    @Column(name = "is_completed", nullable = false)
    private Boolean isCompleted;

    @Column(name = "is_won", nullable = false)
    private Boolean isWon;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "xp_earned", nullable = false)
    private Integer xpEarned;

    @Column(name = "bonus_xp")
    private Integer bonusXp;

    @Column(name = "played_at", nullable = false)
    private LocalDateTime playedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private static final ZoneId ALMATY_ZONE = ZoneId.of("Asia/Almaty");

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now(ALMATY_ZONE);
        if (playedAt == null) playedAt = LocalDateTime.now(ALMATY_ZONE);
    }

    /**
     * Автоматический расчет XP с учетом множителя от наград
     */
    public void calculateXpWithMultiplier(double multiplier) {
        int baseXp = gameType.getBaseXp();
        int performanceBonus = 0;

        if (Boolean.TRUE.equals(isWon)) performanceBonus += 30;
        if (Boolean.TRUE.equals(isCompleted)) performanceBonus += gameType.getCompletionBonus();

        int totalBeforeMultiplier = baseXp + performanceBonus;
        this.xpEarned = (int) (totalBeforeMultiplier * multiplier);
        this.bonusXp = performanceBonus + (int) (totalBeforeMultiplier * (multiplier - 1.0));
    }

    public enum GameType {
        DONUT_GAME(
                "Пончик-раннер",
                "Собирайте пончики и избегайте препятствий",
                120, // Повысил награду
                40
        ),
        NUMBER_SEQUENCE_GAME(
                "Числовая последовательность",
                "Найдите числа в правильном порядке на скорость",
                80,
                25
        );

        private final String displayName;
        private final String description;
        private final int baseXp;
        private final int completionBonus;

        GameType(String displayName, String description, int baseXp, int completionBonus) {
            this.displayName = displayName;
            this.description = description;
            this.baseXp = baseXp;
            this.completionBonus = completionBonus;
        }

        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
        public int getBaseXp() { return baseXp; }
        public int getCompletionBonus() { return completionBonus; }
    }
}
