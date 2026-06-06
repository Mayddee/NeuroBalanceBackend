package org.example.ainote.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ainote.dto.NoteAiResponse;
import org.example.ainote.entity.Note;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * AI service for note analysis using Groq (llama3-8b-8192 via Spring AI).
 *
 * Three operations:
 *  1. summarize  — concise 1-2 sentence summary of a note
 *  2. analyze    — emotional tone + themes + wellness insight + suggestion
 *  3. chat       — free-form Q&A with optional note context
 *
 * All operations respond in the same language as the note/question.
 * Existing note CRUD is NOT touched here.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NoteAiService {

    private final NoteService noteService;
    private final ChatModel chatModel;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ─────────────────────────────────────────────────────────────
    // 1. SUMMARIZE
    // ─────────────────────────────────────────────────────────────

    /**
     * Returns a 1-2 sentence summary of a specific note.
     * Responds in the same language as the note.
     */
    public NoteAiResponse summarize(Long userId, Long noteId) {
        Note note = noteService.getNote(userId, noteId);

        String prompt = """
                [LANGUAGE — MANDATORY] Detect the language of the note below. \
                Your ENTIRE response MUST be written in that EXACT language. \
                Russian note → Russian response. Kazakh note → Kazakh response. \
                English note → English response. NEVER default to English for non-English notes.

                You are a personal wellness journaling assistant in NeuroBalance — \
                a cognitive health tracking app.

                Summarize the following journal note in 1–2 sentences.
                Output ONLY the summary — no introductions, labels, or extra text.
                Keep it concise and capture the core message.

                Note title: %s
                Note content:
                %s
                """.formatted(
                blankIfNull(note.getTitle()),
                blankIfNull(note.getContent())
        );

        String summary = callAi(prompt);
        log.info("Summarized note {} for user {}", noteId, userId);

        return NoteAiResponse.builder()
                .noteId(noteId)
                .noteTitle(note.getTitle())
                .type("summary")
                .summary(summary)
                .generatedAt(LocalDateTime.now())
                .build();
    }

    // ─────────────────────────────────────────────────────────────
    // 2. WELLNESS ANALYSIS
    // ─────────────────────────────────────────────────────────────

    /**
     * Full wellness analysis of a note:
     * emotional tone, themes, wellness insight, actionable suggestion, summary.
     * Tries to parse the AI's JSON reply; falls back gracefully if parsing fails.
     */
    public NoteAiResponse analyzeWellness(Long userId, Long noteId) {
        Note note = noteService.getNote(userId, noteId);

        String prompt = """
                [LANGUAGE — MANDATORY] Detect the language of the note below. \
                ALL JSON field values MUST be written in that EXACT language. \
                Russian note → Russian values. Kazakh note → Kazakh values. \
                English note → English values. NEVER write field values in English if the note is in another language.

                You are a mental wellness AI integrated in NeuroBalance — a cognitive health \
                tracking app that measures three daily metrics:
                  • M-Rest   (sleep & recovery quality, 0-100)
                  • M-Ready  (cognitive readiness & energy, 0-100)
                  • M-Balance (emotional balance & stress, 0-100)

                Analyze the journal note below and respond with ONLY a valid JSON object \
                — no markdown, no backticks, no extra text outside the JSON.

                Required JSON structure:
                {
                  "tone": "<1-2 words describing the emotional tone>",
                  "themes": ["<theme1>", "<theme2>", "<theme3>"],
                  "wellnessInsight": "<1 sentence connecting this note to M-Rest/M-Ready/M-Balance>",
                  "suggestion": "<1 specific, realistic 10-15 minute wellness action for today>",
                  "summary": "<1-2 sentence summary of the note>"
                }

                Additional rules:
                - Be empathetic, non-judgmental, and practical.
                - Keep themes as short keywords (1-4 words each).
                - Wellness insight must reference at least one of: sleep quality, energy level, \
                  stress management, mood, physical activity, or social connection.
                - Suggestion must be doable in under 15 minutes.

                Note title: %s
                Note content:
                %s
                """.formatted(
                blankIfNull(note.getTitle()),
                blankIfNull(note.getContent())
        );

        String rawResponse = callAi(prompt);
        log.info("Wellness analysis raw AI response for note {}: {}", noteId,
                rawResponse.length() > 120 ? rawResponse.substring(0, 120) + "…" : rawResponse);

        return parseAnalysisResponse(rawResponse, noteId, note.getTitle());
    }

    // ─────────────────────────────────────────────────────────────
    // 3. CHAT
    // ─────────────────────────────────────────────────────────────

    /**
     * Free-form AI chat.
     * If noteId is provided the note is injected as context; otherwise the AI
     * answers from the message alone.
     */
    public NoteAiResponse chat(Long userId, String message, Long noteId) {
        String noteContext = "";
        String noteTitleForResponse = null;

        if (noteId != null) {
            Note note = noteService.getNote(userId, noteId);
            noteTitleForResponse = note.getTitle();
            noteContext = """

                    Journal note context:
                    Title: %s
                    Content:
                    %s
                    """.formatted(
                    blankIfNull(note.getTitle()),
                    blankIfNull(note.getContent())
            );
        }

        String prompt = """
                [LANGUAGE — MANDATORY] Detect the language of the user's message below. \
                Your ENTIRE answer MUST be in that EXACT language. \
                Russian message → Russian answer. Kazakh message → Kazakh answer. \
                English message → English answer. NEVER respond in English if the message is in another language.

                You are a helpful wellness AI assistant inside NeuroBalance — a personal \
                mental health journaling app that tracks sleep (M-Rest), cognitive readiness \
                (M-Ready), and emotional balance (M-Balance).
                %s
                User question / message:
                %s

                Rules:
                - Be concise (2-5 sentences), empathetic, and practical.
                - If the question relates to health, reference sleep, energy, stress, or mood where relevant.
                - Only output the answer — no preamble, no labels, no meta-commentary.
                """.formatted(noteContext, message);

        String answer = callAi(prompt);
        log.info("Chat response generated for user {} (noteId={})", userId, noteId);

        return NoteAiResponse.builder()
                .noteId(noteId)
                .noteTitle(noteTitleForResponse)
                .type("chat")
                .answer(answer)
                .generatedAt(LocalDateTime.now())
                .build();
    }

    // ─────────────────────────────────────────────────────────────
    // INTERNAL HELPERS
    // ─────────────────────────────────────────────────────────────

    // ─────────────────────────────────────────────────────────────
    // RAW-TEXT OVERLOADS for JournalEntry AI (no Note entity dependency)
    // ─────────────────────────────────────────────────────────────

    /**
     * Summarize by raw title+content (used by JournalEntryController).
     * referenceId = journalEntry.id for the response DTO.
     */
    public NoteAiResponse summarizeRaw(Long userId, Long referenceId, String title, String content) {
        String prompt = """
                [LANGUAGE — MANDATORY] Detect the language of the note below. \
                Your ENTIRE response MUST be in that EXACT language. \
                Russian note → Russian response. Kazakh note → Kazakh response. \
                English note → English response. NEVER default to English for non-English notes.

                You are a personal wellness journaling assistant in NeuroBalance — \
                a cognitive health tracking app.

                Summarize the following journal note in 1–2 sentences.
                Output ONLY the summary — no introductions, labels, or extra text.
                Keep it concise and capture the core message.

                Note title: %s
                Note content:
                %s
                """.formatted(blankIfNull(title), blankIfNull(content));

        String summary = callAi(prompt);
        log.info("Summarized journal entry {} for user {}", referenceId, userId);
        return NoteAiResponse.builder()
                .noteId(referenceId)
                .noteTitle(title)
                .type("summary")
                .summary(summary)
                .generatedAt(java.time.LocalDateTime.now())
                .build();
    }

    /**
     * Full wellness analysis by raw title+content (used by JournalEntryController).
     */
    public NoteAiResponse analyzeWellnessRaw(Long userId, Long referenceId, String title, String content) {
        String prompt = """
                [LANGUAGE — MANDATORY] Detect the language of the note below. \
                ALL JSON field values MUST be in that EXACT language. \
                Russian note → Russian values. Kazakh note → Kazakh values. \
                English note → English values. NEVER write English values for non-English notes.

                You are a mental wellness AI integrated in NeuroBalance — a cognitive health \
                tracking app that measures three daily metrics:
                  • M-Rest   (sleep & recovery quality, 0-100)
                  • M-Ready  (cognitive readiness & energy, 0-100)
                  • M-Balance (emotional balance & stress, 0-100)

                Analyze the journal note below and respond with ONLY a valid JSON object \
                — no markdown, no backticks, no extra text outside the JSON.

                Required JSON structure:
                {
                  "tone": "<1-2 words describing the emotional tone>",
                  "themes": ["<theme1>", "<theme2>", "<theme3>"],
                  "wellnessInsight": "<1 sentence connecting this note to M-Rest/M-Ready/M-Balance>",
                  "suggestion": "<1 specific, realistic 10-15 minute wellness action for today>",
                  "summary": "<1-2 sentence summary of the note>"
                }

                Additional rules:
                - Be empathetic, non-judgmental, and practical.
                - Keep themes as short keywords (1-4 words each).
                - Suggestion must be doable in under 15 minutes.

                Note title: %s
                Note content:
                %s
                """.formatted(blankIfNull(title), blankIfNull(content));

        String raw = callAi(prompt);
        log.info("Wellness analysis for journal entry {} completed", referenceId);
        return parseAnalysisResponse(raw, referenceId, title);
    }

    /**
     * Chat with raw note context (used by JournalEntryController).
     * If title+content are null, answers from message alone.
     */
    public NoteAiResponse chatRaw(Long userId, Long referenceId,
                                   String title, String content, String message) {
        String noteContext = (title != null || content != null)
                ? """

                    Journal entry context:
                    Title: %s
                    Content:
                    %s
                    """.formatted(blankIfNull(title), blankIfNull(content))
                : "";

        String prompt = """
                [LANGUAGE — MANDATORY] Detect the language of the user's message below. \
                Your ENTIRE answer MUST be in that EXACT language. \
                Russian message → Russian answer. Kazakh message → Kazakh answer. \
                English message → English answer. NEVER respond in English if the message is in another language.

                You are a helpful wellness AI assistant inside NeuroBalance — a personal \
                mental health journaling app that tracks sleep (M-Rest), cognitive readiness \
                (M-Ready), and emotional balance (M-Balance).
                %s
                User question / message:
                %s

                Rules:
                - Be concise (2-5 sentences), empathetic, and practical.
                - If the question relates to health, reference sleep, energy, stress, or mood where relevant.
                - Only output the answer — no preamble, no labels, no meta-commentary.
                """.formatted(noteContext, message);

        String answer = callAi(prompt);
        log.info("Chat response for journal {} generated for user {}", referenceId, userId);
        return NoteAiResponse.builder()
                .noteId(referenceId)
                .noteTitle(title)
                .type("chat")
                .answer(answer)
                .generatedAt(java.time.LocalDateTime.now())
                .build();
    }

    // ─────────────────────────────────────────────────────────────

    private String callAi(String prompt) {
        try {
            ChatClient client = ChatClient.builder(chatModel).build();
            return client.prompt().user(prompt).call().content();
        } catch (Exception e) {
            log.error("AI call failed: {}", e.getMessage());
            throw new RuntimeException("AI service temporarily unavailable. Please try again.");
        }
    }

    /**
     * Parses the JSON analysis response from the AI.
     * Falls back gracefully if the model returns non-JSON text.
     */
    private NoteAiResponse parseAnalysisResponse(String raw, Long noteId, String noteTitle) {
        try {
            // Strip possible markdown code fences the model might accidentally add
            String cleaned = raw.trim();
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.replaceAll("^```[a-zA-Z]*\\n?", "").replaceAll("```$", "").trim();
            }

            JsonNode root = objectMapper.readTree(cleaned);

            String tone = textOrEmpty(root, "tone");
            List<String> themes = parseThemes(root.get("themes"));
            String wellnessInsight = textOrEmpty(root, "wellnessInsight");
            String suggestion = textOrEmpty(root, "suggestion");
            String summary = textOrEmpty(root, "summary");

            return NoteAiResponse.builder()
                    .noteId(noteId)
                    .noteTitle(noteTitle)
                    .type("analysis")
                    .tone(tone)
                    .themes(themes)
                    .wellnessInsight(wellnessInsight)
                    .suggestion(suggestion)
                    .summary(summary)
                    .generatedAt(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.warn("AI returned non-JSON for analysis (note {}), using raw text as insight: {}",
                    noteId, e.getMessage());

            // Graceful fallback: return raw text as wellnessInsight
            return NoteAiResponse.builder()
                    .noteId(noteId)
                    .noteTitle(noteTitle)
                    .type("analysis")
                    .wellnessInsight(raw)
                    .generatedAt(LocalDateTime.now())
                    .build();
        }
    }

    private String textOrEmpty(JsonNode root, String field) {
        JsonNode node = root.get(field);
        return (node != null && !node.isNull()) ? node.asText() : "";
    }

    private List<String> parseThemes(JsonNode themesNode) {
        List<String> result = new ArrayList<>();
        if (themesNode != null && themesNode.isArray()) {
            themesNode.forEach(n -> result.add(n.asText()));
        }
        return result;
    }

    private String blankIfNull(String s) {
        return s != null ? s : "";
    }
}
