package org.example.ainote.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ainote.dto.NoteAiRequest;
import org.example.ainote.dto.NoteAiResponse;
import org.example.ainote.service.NoteAiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * AI-powered endpoints for notes.
 * All routes require JWT authentication (userId from request attribute).
 *
 * Existing note CRUD lives in NoteController and UserController — untouched.
 *
 * New endpoints:
 *   POST /api/v1/notes/{id}/ai/summary   — 1-2 sentence summary
 *   POST /api/v1/notes/{id}/ai/analyze   — tone, themes, wellness insight, suggestion
 *   POST /api/v1/notes/ai/chat           — free-form Q&A with optional note context
 */
@RestController
@RequestMapping("/api/v1/notes")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Note AI", description = "AI-powered note summarisation, wellness analysis, and chat")
public class NoteAiController {

    private final NoteAiService noteAiService;

    // ─────────────────────────────────────────────────────────────
    // 1. SUMMARIZE
    // ─────────────────────────────────────────────────────────────

    /**
     * POST /api/v1/notes/{id}/ai/summary
     *
     * Returns a concise 1–2 sentence summary of the specified note.
     * The summary is always in the same language as the note.
     *
     * Example response:
     * {
     *   "noteId": 5,
     *   "noteTitle": "Плохой день",
     *   "type": "summary",
     *   "summary": "Пользователь описывает усталость и высокий уровень стресса на работе.",
     *   "generatedAt": "2026-05-30 14:30:00"
     * }
     */
    @PostMapping("/{id}/ai/summary")
    @Operation(
            summary = "Summarise a note",
            description = "Returns a concise 1–2 sentence summary of the note in its original language. " +
                    "Powered by Groq llama3-8b-8192."
    )
    public ResponseEntity<NoteAiResponse> summarize(
            HttpServletRequest request,
            @PathVariable Long id
    ) {
        Long userId = getUserId(request);
        log.info("POST /notes/{}/ai/summary - User {}", id, userId);

        NoteAiResponse response = noteAiService.summarize(userId, id);
        return ResponseEntity.ok(response);
    }

    // ─────────────────────────────────────────────────────────────
    // 2. WELLNESS ANALYSIS
    // ─────────────────────────────────────────────────────────────

    /**
     * POST /api/v1/notes/{id}/ai/analyze
     *
     * Full wellness analysis of a note:
     *   • tone          — emotional tone in 1-2 words
     *   • themes        — 2-3 keywords from the note
     *   • wellnessInsight — how the note relates to M-Rest/M-Ready/M-Balance
     *   • suggestion    — one actionable wellness tip for today
     *   • summary       — short note summary
     *
     * All fields are returned in the same language as the note.
     *
     * Example response:
     * {
     *   "noteId": 5,
     *   "noteTitle": "Плохой день",
     *   "type": "analysis",
     *   "tone": "тревожный",
     *   "themes": ["рабочий стресс", "усталость", "нехватка сна"],
     *   "wellnessInsight": "Высокий уровень стресса и усталость указывают на сниженный M-Balance и M-Ready.",
     *   "suggestion": "Сделайте 10-минутную прогулку на свежем воздухе после обеда.",
     *   "summary": "Пользователь испытывает стресс из-за дедлайнов и плохо спал.",
     *   "generatedAt": "2026-05-30 14:31:00"
     * }
     */
    @PostMapping("/{id}/ai/analyze")
    @Operation(
            summary = "Full wellness analysis of a note",
            description = "Returns emotional tone, main themes, a wellness insight connecting the note " +
                    "to M-Rest/M-Ready/M-Balance metrics, an actionable suggestion, and a summary. " +
                    "All values are in the note's original language."
    )
    public ResponseEntity<NoteAiResponse> analyze(
            HttpServletRequest request,
            @PathVariable Long id
    ) {
        Long userId = getUserId(request);
        log.info("POST /notes/{}/ai/analyze - User {}", id, userId);

        NoteAiResponse response = noteAiService.analyzeWellness(userId, id);
        return ResponseEntity.ok(response);
    }

    // ─────────────────────────────────────────────────────────────
    // 3. CHAT
    // ─────────────────────────────────────────────────────────────

    /**
     * POST /api/v1/notes/ai/chat
     *
     * Conversational AI assistant.
     * Provide a message (required) and an optional noteId for context.
     *
     * Request body:
     * {
     *   "message": "Как мне улучшить сон согласно этой заметке?",
     *   "noteId": 5          ← optional
     * }
     *
     * Example response:
     * {
     *   "noteId": 5,
     *   "noteTitle": "Плохой день",
     *   "type": "chat",
     *   "answer": "Судя по заметке, вам стоит попробовать ложиться в 23:00 и избегать экрана за час до сна.",
     *   "generatedAt": "2026-05-30 14:32:00"
     * }
     */
    @PostMapping("/ai/chat")
    @Operation(
            summary = "AI chat assistant for notes",
            description = "Ask the AI anything about a specific note (provide noteId) or general " +
                    "wellness questions. Responds in the same language as the user's message."
    )
    public ResponseEntity<NoteAiResponse> chat(
            HttpServletRequest request,
            @Valid @RequestBody NoteAiRequest body
    ) {
        Long userId = getUserId(request);
        log.info("POST /notes/ai/chat - User {} (noteId={})", userId, body.getNoteId());

        NoteAiResponse response = noteAiService.chat(userId, body.getMessage(), body.getNoteId());
        return ResponseEntity.ok(response);
    }

    // ─────────────────────────────────────────────────────────────

    private Long getUserId(HttpServletRequest request) {
        return (Long) request.getAttribute("userId");
    }
}
