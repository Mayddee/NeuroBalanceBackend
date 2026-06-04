package org.example.ainote.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Response DTO for JournalEntry.
 * moodEmoji вычисляется из moodScore на стороне сервиса.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JournalEntryResponse {

    private Long id;
    private Long userId;

    private String title;
    private String content;

    private Integer moodScore;
    private String moodEmoji;      // 😢 😔 😐 😊 😄

    private String tags;           // comma-separated

    private Boolean isFavorite;
    private Integer wordCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
