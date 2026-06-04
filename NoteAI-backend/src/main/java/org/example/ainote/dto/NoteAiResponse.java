package org.example.ainote.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Unified response DTO for all AI endpoints.
 * Fields are populated selectively depending on the operation type:
 *   "summary"  → summary
 *   "analysis" → summary, tone, themes, wellnessInsight, suggestion
 *   "chat"     → answer
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NoteAiResponse {

    /** The note this response relates to (null for chat without a specific note). */
    private Long noteId;
    private String noteTitle;

    /** "summary" | "analysis" | "chat" */
    private String type;

    // ── SUMMARY ─────────────────────────────────────────────────────
    /** 1–2 sentence summary of the note content. */
    private String summary;

    // ── ANALYSIS ────────────────────────────────────────────────────
    /** Emotional tone in 1–2 words (e.g. "anxious", "motivated"). */
    private String tone;

    /** 2–3 short theme keywords extracted from the note. */
    private List<String> themes;

    /**
     * 1-sentence insight connecting the note to M-Rest / M-Ready / M-Balance
     * metrics tracked in NeuroBalance.
     */
    private String wellnessInsight;

    /** 1 specific, actionable 10–15 min wellness suggestion for today. */
    private String suggestion;

    // ── CHAT ────────────────────────────────────────────────────────
    /** AI answer to the user's free-form question. */
    private String answer;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime generatedAt;
}
