package org.example.nbcheckinservice.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Character entity - represents the companion characters (Moa, Bul, Piko, Sparky)
 * Each character has 5 growth stages based on user level
 */
@Entity
@Table(name = "characters")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Character {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, unique = true, length = 50)
    private String name; // "MOA", "BUL", "PIKO", "SPARKY"

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName; // Localized display name

    @Column(name = "description", columnDefinition = "TEXT")
    private String description; // Character backstory

    @Column(name = "personality_trait", length = 255)
    private String personalityTrait; // "Calm and attentive", "Quiet and thoughtful", etc.

    // Growth stages configuration (stored as JSON or separate table)
    @Column(name = "stage_1_image_url", length = 500)
    private String stage1ImageUrl;

    @Column(name = "stage_2_image_url", length = 500)
    private String stage2ImageUrl;

    @Column(name = "stage_3_image_url", length = 500)
    private String stage3ImageUrl;

    @Column(name = "stage_4_image_url", length = 500)
    private String stage4ImageUrl;

    @Column(name = "stage_5_image_url", length = 500)
    private String stage5ImageUrl;

    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private Boolean isDefault = false; // MOA is default

    @Column(name = "unlock_level")
    private Integer unlockLevel; // Level required to unlock character (null = unlocked from start)

    /**
     * Get image URL for specific stage
     */
    public String getImageUrlForStage(Integer stage) {
        return switch (stage) {
            case 1 -> stage1ImageUrl;
            case 2 -> stage2ImageUrl;
            case 3 -> stage3ImageUrl;
            case 4 -> stage4ImageUrl;
            case 5 -> stage5ImageUrl;
            default -> stage1ImageUrl;
        };
    }

    /**
     * Character types enum for consistency
     */
    public enum CharacterType {
        MOA("Моа", "Рост не нужна спешка", "Calm and mindful, helps slow down and find inner balance"),
        BUL("Буль", "Про спокойствие и баланс", "Quiet and thoughtful, helps listen to yourself and find inner balance"),
        PIKO("Пико", "Активность и движение", "Active and energetic, encourages movement and activity"),
        SPARKY("Спарки", "Креативность и радость", "Creative and joyful, inspires creativity and positive emotions");

        private final String displayName;
        private final String tagline;
        private final String description;

        CharacterType(String displayName, String tagline, String description) {
            this.displayName = displayName;
            this.tagline = tagline;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getTagline() {
            return tagline;
        }

        public String getDescription() {
            return description;
        }
    }
}