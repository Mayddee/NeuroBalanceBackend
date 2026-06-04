package org.example.ainote.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Enhanced journal entry — рядом со старой таблицей notes, не заменяет её.
 *
 * Дополнительные поля по сравнению с Note:
 *   userId      — явная привязка к пользователю (не через junction-таблицу)
 *   moodScore   — 1-5, опционально
 *   tags        — теги через запятую
 *   isFavorite  — закладка
 *   wordCount   — вычисляется при сохранении
 *   createdAt   — дата создания (не изменяется)
 *   updatedAt   — дата последнего изменения (обновляется триггером в БД)
 */
@Entity
@Table(name = "journal_entries")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JournalEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "title", length = 500)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "mood_score")
    private Integer moodScore;

    @Column(name = "tags", length = 500)
    private String tags;

    @Column(name = "is_favorite", nullable = false)
    @Builder.Default
    private Boolean isFavorite = false;

    @Column(name = "word_count", nullable = false)
    @Builder.Default
    private Integer wordCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        this.wordCount = countWords(this.content);
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        this.wordCount = countWords(this.content);
    }

    private static int countWords(String text) {
        if (text == null || text.isBlank()) return 0;
        return text.trim().split("\\s+").length;
    }

    public static String moodEmoji(Integer score) {
        if (score == null) return null;
        return switch (score) {
            case 1 -> "😢";
            case 2 -> "😔";
            case 3 -> "😐";
            case 4 -> "😊";
            case 5 -> "😄";
            default -> null;
        };
    }
}
