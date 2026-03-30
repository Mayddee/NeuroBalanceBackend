package org.example.nbcheckinservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity representing gamification game sessions
 * These are FUN games for engagement, not cognitive tests
 */
@Entity
@Table(name = "game_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    @NotNull(message = "User ID is required")
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "game_type", nullable = false)
    @NotNull(message = "Game type is required")
    private GameType gameType;

    // ========== GAME RESULTS ==========

    @Column(name = "is_completed", nullable = false)
    @Builder.Default
    private Boolean isCompleted = false;

    @Column(name = "is_won", nullable = false)
    @Builder.Default
    private Boolean isWon = false;

    @Column(name = "duration_seconds")
    @Min(value = 0, message = "Duration cannot be negative")
    private Integer durationSeconds;

    // ========== XP & REWARDS ==========

    @Column(name = "xp_earned", nullable = false)
    @Builder.Default
    private Integer xpEarned = 0;

    @Column(name = "bonus_xp")
    private Integer bonusXp;

    // ========== METADATA ==========

    @Column(name = "played_at", nullable = false)
    private LocalDateTime playedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (playedAt == null) {
            playedAt = LocalDateTime.now();
        }
    }

    /**
     * Calculate XP based on game completion
     */
    public void calculateXpEarned() {
        int baseXp = gameType.getBaseXp();
        int performanceBonus = 0;

        // Win bonus
        if (Boolean.TRUE.equals(isWon)) {
            performanceBonus += 30;
        }

        // Completion bonus (even if didn't win)
        if (Boolean.TRUE.equals(isCompleted)) {
            performanceBonus += gameType.getCompletionBonus();
        }

        this.bonusXp = performanceBonus;
        this.xpEarned = baseXp + performanceBonus;
    }

    // ========== GAME TYPES ==========

    public enum GameType {
        DONUT_GAME(
                "Продолжить игру",
                "Catch donuts and avoid obstacles",
                100,
                30
        ),
        CHARACTER_CARE(
                "Уход за персонажем",
                "Feed and play with your character",
                50,
                20
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

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }

        public int getBaseXp() {
            return baseXp;
        }

        public int getCompletionBonus() {
            return completionBonus;
        }
    }
}