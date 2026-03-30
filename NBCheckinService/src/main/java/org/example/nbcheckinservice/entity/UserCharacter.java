package org.example.nbcheckinservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity representing user's chosen character/pet
 */
@Entity
@Table(name = "user_characters")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCharacter {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    @NotNull
    private Long userId;

    // ========== CHARACTER INFO ==========

    @Enumerated(EnumType.STRING)
    @Column(name = "character_type", nullable = false)
    @NotNull(message = "Character type is required")
    @Builder.Default
    private CharacterType characterType = CharacterType.MOA;

    @Column(name = "character_name", length = 50)
    private String characterName; // User can name their pet

    // ========== LEVEL SYSTEM ==========

    @Column(name = "current_level", nullable = false)
    @Min(value = 1, message = "Level must be at least 1")
    @Max(value = 5, message = "Level cannot exceed 5")
    @Builder.Default
    private Integer currentLevel = 1;

    @Column(name = "total_xp", nullable = false)
    @Min(value = 0, message = "XP cannot be negative")
    @Builder.Default
    private Integer totalXp = 0;

    @Column(name = "xp_for_next_level", nullable = false)
    @Builder.Default
    private Integer xpForNextLevel = 500; // Level 2 requires 500 XP

    // ========== CHARACTER STATE ==========

    @Column(name = "happiness_level", nullable = false)
    @Min(value = 0, message = "Happiness cannot be negative")
    @Max(value = 100, message = "Happiness cannot exceed 100")
    @Builder.Default
    private Integer happinessLevel = 50;

    @Column(name = "energy_level", nullable = false)
    @Min(value = 0, message = "Energy cannot be negative")
    @Max(value = 100, message = "Energy cannot exceed 100")
    @Builder.Default
    private Integer energyLevel = 100;

    // ========== METADATA ==========

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "last_interaction_at")
    private LocalDateTime lastInteractionAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        lastInteractionAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ========== BUSINESS LOGIC ==========

    /**
     * Add XP and check for level up
     * Returns true if leveled up
     */
    public boolean addXp(int xpToAdd) {
        this.totalXp += xpToAdd;

        // Check if level up
        if (this.totalXp >= this.xpForNextLevel && this.currentLevel < 5) {
            levelUp();
            return true;
        }

        return false;
    }

    /**
     * Level up the character
     */
    private void levelUp() {
        if (this.currentLevel >= 5) return; // Max level reached

        this.currentLevel++;
        this.xpForNextLevel = calculateXpForNextLevel(this.currentLevel);
        this.happinessLevel = Math.min(100, this.happinessLevel + 10);
        this.energyLevel = 100; // Restore energy on level up
    }

    /**
     * Calculate XP required for next level
     * Level 1→2: 500 XP
     * Level 2→3: 1000 XP
     * Level 3→4: 2000 XP
     * Level 4→5: 3500 XP
     */
    private int calculateXpForNextLevel(int level) {
        return switch (level) {
            case 1 -> 500;
            case 2 -> 1000;
            case 3 -> 2000;
            case 4 -> 3500;
            case 5 -> 0; // Max level, no next level
            default -> 500;
        };
    }

    /**
     * Get progress to next level as percentage
     */
    public double getLevelProgressPercentage() {
        if (currentLevel >= 5) return 100.0;

        int previousLevelXp = calculateXpForPreviousLevel(currentLevel);
        int currentLevelXp = totalXp - previousLevelXp;
        int xpNeeded = xpForNextLevel - previousLevelXp;

        return (currentLevelXp * 100.0) / xpNeeded;
    }

    private int calculateXpForPreviousLevel(int level) {
        return switch (level) {
            case 1 -> 0;
            case 2 -> 500;
            case 3 -> 1500;
            case 4 -> 3500;
            case 5 -> 7000;
            default -> 0;
        };
    }

    /**
     * Get character description based on type and level
     */
    public String getCharacterDescription() {
        return characterType.getDescription(currentLevel);
    }

    /**
     * Get character emoji based on level
     */
    public String getCharacterEmoji() {
        return characterType.getEmoji(currentLevel);
    }

    /**
     * Update happiness based on check-in quality
     */
    public void updateHappiness(double wellnessScore) {
        int happinessChange =
                wellnessScore >= 80 ? 5 :
                        wellnessScore >= 60 ? 3 :
                                wellnessScore >= 40 ? 1 : -2;

        this.happinessLevel = Math.max(0, Math.min(100, this.happinessLevel + happinessChange));
        this.lastInteractionAt = LocalDateTime.now();
    }

    // ========== CHARACTER TYPES ==========

    public enum CharacterType {
        MOA("Моа", "Про спокойствие и баланс"),
        BUL("Буль", "Про спокойствие и баланс"),
        PIKO("Пико", "Про спокойствие и баланс"),
        SPARKY("Спарки", "Про спокойствие и баланс");

        private final String displayName;
        private final String baseDescription;

        CharacterType(String displayName, String baseDescription) {
            this.displayName = displayName;
            this.baseDescription = baseDescription;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription(int level) {
            return switch (level) {
                case 1 -> displayName + " ещё совсем маленький и почти незаметный.";
                case 2 -> displayName + " подрос и стал увереннее.";
                case 3 -> displayName + " активно растёт и развивается.";
                case 4 -> displayName + " стал сильным и мудрым.";
                case 5 -> displayName + " достиг максимального уровня! Невероятно!";
                default -> baseDescription;
            };
        }

        public String getEmoji(int level) {
            return switch (this) {
                case MOA -> switch (level) {
                    case 1 -> "🌱";
                    case 2 -> "🌿";
                    case 3 -> "🍀";
                    case 4 -> "🌳";
                    case 5 -> "🌲";
                    default -> "🌱";
                };
                case BUL -> switch (level) {
                    case 1 -> "💧";
                    case 2 -> "💦";
                    case 3 -> "🌊";
                    case 4 -> "🌀";
                    case 5 -> "⭐";
                    default -> "💧";
                };
                case PIKO -> switch (level) {
                    case 1 -> "🎈";
                    case 2 -> "🎀";
                    case 3 -> "🎁";
                    case 4 -> "🎉";
                    case 5 -> "🏆";
                    default -> "🎈";
                };
                case SPARKY -> switch (level) {
                    case 1 -> "✨";
                    case 2 -> "⚡";
                    case 3 -> "💫";
                    case 4 -> "🌟";
                    case 5 -> "💎";
                    default -> "✨";
                };
            };
        }
    }
}