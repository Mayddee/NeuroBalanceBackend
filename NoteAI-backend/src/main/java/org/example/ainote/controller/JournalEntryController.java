package org.example.ainote.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ainote.dto.JournalEntryRequest;
import org.example.ainote.dto.JournalEntryResponse;
import org.example.ainote.dto.NoteAiRequest;
import org.example.ainote.dto.NoteAiResponse;
import org.example.ainote.entity.JournalEntry;
import org.example.ainote.service.JournalEntryService;
import org.example.ainote.service.NoteAiService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Расширенный журнал (journal_entries) — отдельная таблица, старые Note не тронуты.
 *
 * Дебаунс-паттерн:
 *   1. Пользователь начинает писать → POST /journal (создание + WRITE_NOTE)
 *   2. Продолжает редактировать → фронтенд ждёт 2-3 сек паузы → PATCH /journal/{id} (auto-save)
 *   3. Задача WRITE_NOTE выполняется только один раз в день (idempotent в NBCheckinService)
 *
 * AI-эндпойнты используют те же Groq-промпты что и NoteAiController,
 * но работают с JournalEntry без зависимости от старого NoteService.
 */
@RestController
@RequestMapping("/api/v1/journal")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Journal", description = "Enhanced journal entries with auto-save, date filters and AI")
public class JournalEntryController {

    private final JournalEntryService journalService;
    private final NoteAiService noteAiService;

    private Long getUserId(HttpServletRequest request) {
        return (Long) request.getAttribute("userId");
    }

    // ═══════════════════════════════════════════════════════════
    //  CRUD
    // ═══════════════════════════════════════════════════════════

    /**
     * POST /api/v1/journal
     *
     * Создаёт новую запись журнала.
     * Автоматически триггерит WRITE_NOTE (+45 XP персонажу) через NBCheckinService.
     * Вызывается один раз когда пользователь начинает новую запись.
     *
     * Тело запроса:
     * {
     *   "title": "Мои мысли",
     *   "content": "...",
     *   "moodScore": 4,
     *   "tags": "работа,идеи",
     *   "isFavorite": false
     * }
     */
    @PostMapping
    @Operation(summary = "Create journal entry (triggers WRITE_NOTE daily task)")
    public ResponseEntity<JournalEntryResponse> create(
            HttpServletRequest request,
            @Valid @RequestBody JournalEntryRequest body
    ) {
        Long userId = getUserId(request);
        String authHeader = request.getHeader("Authorization");
        log.info("POST /journal - User {} creating entry: '{}'", userId, body.getTitle());

        JournalEntryResponse resp = journalService.create(userId, body, authHeader);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    /**
     * GET /api/v1/journal
     *
     * Все записи пользователя, от новых к старым.
     */
    @GetMapping
    @Operation(summary = "Get all journal entries (newest first)")
    public ResponseEntity<List<JournalEntryResponse>> getAll(HttpServletRequest request) {
        Long userId = getUserId(request);
        log.info("GET /journal - User {}", userId);
        return ResponseEntity.ok(journalService.getAll(userId));
    }

    /**
     * GET /api/v1/journal/today
     *
     * Записи за сегодня (Asia/Almaty) по дате создания.
     */
    @GetMapping("/today")
    @Operation(summary = "Get today's journal entries (Asia/Almaty timezone)")
    public ResponseEntity<List<JournalEntryResponse>> getToday(HttpServletRequest request) {
        Long userId = getUserId(request);
        log.info("GET /journal/today - User {}", userId);
        return ResponseEntity.ok(journalService.getToday(userId));
    }

    /**
     * GET /api/v1/journal/date/{date}
     *
     * Записи за конкретную дату (yyyy-MM-dd) по дате создания.
     */
    @GetMapping("/date/{date}")
    @Operation(summary = "Get journal entries for a specific date (by createdAt)")
    public ResponseEntity<List<JournalEntryResponse>> getByDate(
            HttpServletRequest request,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        Long userId = getUserId(request);
        log.info("GET /journal/date/{} - User {}", date, userId);
        return ResponseEntity.ok(journalService.getByDate(userId, date));
    }

    /**
     * GET /api/v1/journal/range?startDate=2026-06-01&endDate=2026-06-04
     *
     * Записи в диапазоне дат (включительно) по дате создания.
     */
    @GetMapping("/range")
    @Operation(summary = "Get journal entries in date range (startDate .. endDate, by createdAt)")
    public ResponseEntity<List<JournalEntryResponse>> getByRange(
            HttpServletRequest request,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        Long userId = getUserId(request);
        log.info("GET /journal/range [{} → {}] - User {}", startDate, endDate, userId);
        return ResponseEntity.ok(journalService.getByDateRange(userId, startDate, endDate));
    }

    /**
     * GET /api/v1/journal/favorites
     *
     * Только избранные записи (isFavorite=true).
     */
    @GetMapping("/favorites")
    @Operation(summary = "Get favorite journal entries")
    public ResponseEntity<List<JournalEntryResponse>> getFavorites(HttpServletRequest request) {
        Long userId = getUserId(request);
        log.info("GET /journal/favorites - User {}", userId);
        return ResponseEntity.ok(journalService.getFavorites(userId));
    }

    /**
     * GET /api/v1/journal/{id}
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get journal entry by ID")
    public ResponseEntity<JournalEntryResponse> getById(
            HttpServletRequest request,
            @PathVariable Long id
    ) {
        Long userId = getUserId(request);
        log.info("GET /journal/{} - User {}", id, userId);
        return ResponseEntity.ok(journalService.getById(userId, id));
    }

    /**
     * PUT /api/v1/journal/{id}
     *
     * Полное обновление. Все поля перезаписываются.
     * Задача WRITE_NOTE НЕ триггерится повторно.
     */
    @PutMapping("/{id}")
    @Operation(summary = "Full update of a journal entry (no task re-trigger)")
    public ResponseEntity<JournalEntryResponse> update(
            HttpServletRequest request,
            @PathVariable Long id,
            @Valid @RequestBody JournalEntryRequest body
    ) {
        Long userId = getUserId(request);
        log.info("PUT /journal/{} - User {}", id, userId);
        return ResponseEntity.ok(journalService.update(userId, id, body));
    }

    /**
     * PATCH /api/v1/journal/{id}
     *
     * Частичное обновление / auto-save.
     * Обновляет только не-null поля из тела.
     * Фронтенд вызывает этот эндпойнт с дебаунсом (2-3 сек после паузы в наборе текста).
     * Задача WRITE_NOTE НЕ триггерится (уже выполнена при CREATE).
     * updatedAt обновляется автоматически (@PreUpdate).
     *
     * Тело запроса (только изменённые поля):
     * {
     *   "content": "...новый текст..."
     * }
     */
    @PatchMapping("/{id}")
    @Operation(
            summary = "Partial update / auto-save (debounce endpoint)",
            description = "Updates only the provided fields. Call with 2-3s debounce from frontend. " +
                    "updatedAt is auto-refreshed. WRITE_NOTE task NOT re-triggered."
    )
    public ResponseEntity<JournalEntryResponse> patch(
            HttpServletRequest request,
            @PathVariable Long id,
            @RequestBody JournalEntryRequest body
    ) {
        Long userId = getUserId(request);
        log.debug("PATCH /journal/{} (auto-save) - User {}", id, userId);
        return ResponseEntity.ok(journalService.patch(userId, id, body));
    }

    /**
     * DELETE /api/v1/journal/{id}
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete journal entry")
    public ResponseEntity<Map<String, Object>> delete(
            HttpServletRequest request,
            @PathVariable Long id
    ) {
        Long userId = getUserId(request);
        log.info("DELETE /journal/{} - User {}", id, userId);
        journalService.delete(userId, id);
        return ResponseEntity.ok(Map.of(
                "message", "Journal entry deleted",
                "id", id
        ));
    }

    // ═══════════════════════════════════════════════════════════
    //  AI ENDPOINTS
    // ═══════════════════════════════════════════════════════════

    /**
     * POST /api/v1/journal/{id}/ai/summary
     *
     * 1-2 предложения резюме записи на языке оригинала.
     * Тело запроса: пустое (AI читает запись по ID).
     *
     * Пример ответа:
     * {
     *   "noteId": 3,
     *   "noteTitle": "Тяжёлый день",
     *   "type": "summary",
     *   "summary": "Автор испытывает стресс от дедлайнов и хронического недосыпания.",
     *   "generatedAt": "2026-06-04 14:00:00"
     * }
     */
    @PostMapping("/{id}/ai/summary")
    @Operation(
            summary = "AI summary of a journal entry",
            description = "1-2 sentence summary in the entry's original language. Powered by Groq llama-3.3-70b-versatile."
    )
    public ResponseEntity<NoteAiResponse> aiSummary(
            HttpServletRequest request,
            @PathVariable Long id
    ) {
        Long userId = getUserId(request);
        log.info("POST /journal/{}/ai/summary - User {}", id, userId);

        JournalEntry entry = journalService.findOwned(userId, id);
        NoteAiResponse resp = noteAiService.summarizeRaw(userId, id, entry.getTitle(), entry.getContent());
        return ResponseEntity.ok(resp);
    }

    /**
     * POST /api/v1/journal/{id}/ai/analyze
     *
     * Полный wellness-анализ: тон, темы, M-Rest/M-Ready/M-Balance инсайт, совет, резюме.
     * Тело запроса: пустое.
     *
     * Пример ответа:
     * {
     *   "noteId": 3,
     *   "noteTitle": "Тяжёлый день",
     *   "type": "analysis",
     *   "tone": "тревожный",
     *   "themes": ["стресс", "недосыпание", "перегрузка"],
     *   "wellnessInsight": "Недосыпание снижает M-Rest ниже 50, а хронический стресс давит на M-Balance.",
     *   "suggestion": "Сделайте 10-минутную прогулку в обед — это снижает кортизол на 15-20%.",
     *   "summary": "Автор перегружен работой и плохо спит, планирует наладить режим.",
     *   "generatedAt": "2026-06-04 14:01:00"
     * }
     */
    @PostMapping("/{id}/ai/analyze")
    @Operation(
            summary = "Full wellness analysis of a journal entry",
            description = "Returns emotional tone, themes, M-Rest/M-Ready/M-Balance wellness insight, " +
                    "actionable suggestion, and summary. All in the entry's original language."
    )
    public ResponseEntity<NoteAiResponse> aiAnalyze(
            HttpServletRequest request,
            @PathVariable Long id
    ) {
        Long userId = getUserId(request);
        log.info("POST /journal/{}/ai/analyze - User {}", id, userId);

        JournalEntry entry = journalService.findOwned(userId, id);
        NoteAiResponse resp = noteAiService.analyzeWellnessRaw(
                userId, id, entry.getTitle(), entry.getContent());
        return ResponseEntity.ok(resp);
    }

    /**
     * POST /api/v1/journal/ai/chat
     *
     * AI-чат. Передай message и (необязательно) journalId для контекста записи.
     *
     * Тело запроса:
     * {
     *   "message": "Что в этой записи влияет на мой M-Balance?",
     *   "noteId": 3
     * }
     *
     * Пример ответа:
     * {
     *   "noteId": 3,
     *   "noteTitle": "Тяжёлый день",
     *   "type": "chat",
     *   "answer": "...",
     *   "generatedAt": "2026-06-04 14:02:00"
     * }
     */
    @PostMapping("/ai/chat")
    @Operation(
            summary = "AI chat about a journal entry",
            description = "Free-form Q&A. Provide noteId to give the AI context of a specific entry. " +
                    "Responds in the same language as the user's message."
    )
    public ResponseEntity<NoteAiResponse> aiChat(
            HttpServletRequest request,
            @Valid @RequestBody NoteAiRequest body
    ) {
        Long userId = getUserId(request);
        Long journalId = body.getNoteId();
        log.info("POST /journal/ai/chat - User {} (journalId={})", userId, journalId);

        String title = null, content = null;
        if (journalId != null) {
            JournalEntry entry = journalService.findOwned(userId, journalId);
            title   = entry.getTitle();
            content = entry.getContent();
        }

        NoteAiResponse resp = noteAiService.chatRaw(userId, journalId, title, content, body.getMessage());
        return ResponseEntity.ok(resp);
    }
}
