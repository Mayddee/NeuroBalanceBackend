package org.example.nbcheckinservice.entity;


import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Entity representing a mood log entry
 * Path: mental-health-service/src/main/java/com/neuralbalance/mentalhealth/entity/MoodLog.java
 */
@Entity
@Table(name = "mood_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MoodLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    @NotNull(message = "User ID is required")
    private Long userId;

    @Column(name = "log_timestamp", nullable = false)
    @NotNull(message = "Timestamp is required")
    private LocalDateTime logTimestamp;

    // ========== MOOD DETAILS ==========

    @Column(name = "mood_value", nullable = false)
    @NotNull(message = "Mood value is required")
    @Min(value = 1, message = "Mood value must be between 1 and 5")
    @Max(value = 5, message = "Mood value must be between 1 and 5")
    private Integer moodValue; // 1=very bad, 2=bad, 3=neutral, 4=good, 5=very good

    @Column(name = "mood_emoji", length = 10)
    private String moodEmoji;

    @Column(name = "mood_label", length = 50)
    private String moodLabel; // happy, sad, anxious, stressed, excited, angry, calm, tired

    // ========== INTENSITY ==========

    @Column(name = "intensity")
    @Min(value = 1, message = "Intensity must be between 1 and 10")
    @Max(value = 10, message = "Intensity must be between 1 and 10")
    private Integer intensity; // How strongly feeling this emotion

    // ========== CONTEXT ==========

    @Column(name = "context_note", columnDefinition = "TEXT")
    private String contextNote;

    @Column(name = "location", length = 100)
    private String location; // home, work, school, gym, outside, transport

    @Column(name = "activity", length = 100)
    private String activity; // working, studying, exercising, socializing, relaxing

    // ========== TRIGGERS (Array) ==========

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "triggers", columnDefinition = "text[]")
    private List<String> triggers; // work_stress, relationship, health, achievement, social

    // ========== PHYSICAL SENSATIONS (Array) ==========

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "physical_sensations", columnDefinition = "text[]")
    private List<String> physicalSensations; // headache, tension, energy, fatigue, restlessness

    // ========== METADATA ==========

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();

        if (logTimestamp == null) {
            logTimestamp = LocalDateTime.now();
        }

        // Set emoji based on mood value
        if (moodEmoji == null && moodValue != null) {
            moodEmoji = getMoodEmojiByValue(moodValue);
        }
    }

    /**
     * Get emoji based on mood value
     */
    public static String getMoodEmojiByValue(Integer value) {
        if (value == null) return "😐";
        return switch (value) {
            case 1 -> "😭"; // Very bad
            case 2 -> "😢"; // Bad
            case 3 -> "😐"; // Neutral
            case 4 -> "🙂"; // Good
            case 5 -> "😊"; // Very good
            default -> "😐";
        };
    }

    /**
     * Get default mood label based on value
     */
    public String getDefaultMoodLabel() {
        if (moodValue == null) return "neutral";
        return switch (moodValue) {
            case 1 -> "very_bad";
            case 2 -> "sad";
            case 3 -> "neutral";
            case 4 -> "happy";
            case 5 -> "very_happy";
            default -> "neutral";
        };
    }
}