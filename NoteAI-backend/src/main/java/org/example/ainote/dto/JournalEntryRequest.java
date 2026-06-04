package org.example.ainote.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request DTO for creating / updating a JournalEntry.
 * На PATCH-запросах (auto-save) все поля необязательны — обновляются только переданные.
 * На POST-запросе title обязателен.
 */
@Data
public class JournalEntryRequest {

    @NotBlank(message = "Title cannot be blank")
    @Size(max = 500, message = "Title must be ≤ 500 characters")
    private String title;

    private String content;

    /** Настроение в момент написания (1–5, необязательно) */
    @Min(value = 1, message = "Mood score must be 1–5")
    @Max(value = 5, message = "Mood score must be 1–5")
    private Integer moodScore;

    /** Теги через запятую, например: "работа,стресс,планирование" */
    @Size(max = 500, message = "Tags must be ≤ 500 characters")
    private String tags;

    private Boolean isFavorite;
}
