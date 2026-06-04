package org.example.ainote.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request DTO for the AI chat endpoint.
 * noteId is optional — if provided the AI receives the full note as context.
 */
@Data
public class NoteAiRequest {

    @NotBlank(message = "Message cannot be blank")
    @Size(max = 2000, message = "Message must not exceed 2000 characters")
    private String message;

    /** Optional — ID of the note to use as conversation context. */
    private Long noteId;
}
