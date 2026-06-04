package org.example.ainote.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ainote.client.CheckinServiceClient;
import org.example.ainote.dto.JournalEntryRequest;
import org.example.ainote.dto.JournalEntryResponse;
import org.example.ainote.entity.JournalEntry;
import org.example.ainote.repository.JournalEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

/**
 * CRUD-сервис для расширенных записей журнала (journal_entries).
 *
 * Старые Note/NoteService НЕ затронуты.
 *
 * Дебаунс-логика:
 *   - POST  /journal       → создаёт запись + WRITE_NOTE задача (один раз)
 *   - PATCH /journal/{id}  → частичное обновление (auto-save, без повторного тригг. задачи)
 *   - PUT   /journal/{id}  → полное обновление (без тригг. задачи)
 *
 * Фронтенд реализует дебаунс: ждёт 2-3 сек после ввода → вызывает PATCH.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JournalEntryService {

    private static final ZoneId ALMATY = ZoneId.of("Asia/Almaty");

    private final JournalEntryRepository repo;
    private final CheckinServiceClient checkinServiceClient;

    // ─────────────────────────────────────────────────────────────
    // CREATE
    // ─────────────────────────────────────────────────────────────

    /**
     * Создаёт новую запись журнала.
     * После сохранения асинхронно уведомляет NBCheckinService
     * → задача WRITE_NOTE помечается выполненной (+45 XP персонажу).
     *
     * @param authHeader полный "Bearer <token>" из оригинального запроса
     */
    @Transactional
    public JournalEntryResponse create(Long userId, JournalEntryRequest req, String authHeader) {
        log.info("Creating journal entry for user {}: '{}'", userId, req.getTitle());

        JournalEntry entry = JournalEntry.builder()
                .userId(userId)
                .title(req.getTitle())
                .content(req.getContent())
                .moodScore(req.getMoodScore())
                .tags(req.getTags())
                .isFavorite(req.getIsFavorite() != null && req.getIsFavorite())
                .build();

        entry = repo.save(entry);
        log.info("Journal entry created: id={}, wordCount={}", entry.getId(), entry.getWordCount());

        // Async — fire & forget, не влияет на ответ если checkin-сервис недоступен
        checkinServiceClient.notifyNoteWritten(authHeader);

        return toResponse(entry);
    }

    // ─────────────────────────────────────────────────────────────
    // READ
    // ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public JournalEntryResponse getById(Long userId, Long id) {
        return toResponse(findOwned(userId, id));
    }

    @Transactional(readOnly = true)
    public List<JournalEntryResponse> getAll(Long userId) {
        return repo.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    /**
     * Все записи за сегодня (Asia/Almaty) по createdAt.
     */
    @Transactional(readOnly = true)
    public List<JournalEntryResponse> getToday(Long userId) {
        LocalDate today = LocalDate.now(ALMATY);
        return getByDate(userId, today);
    }

    /**
     * Все записи за конкретную дату по createdAt.
     */
    @Transactional(readOnly = true)
    public List<JournalEntryResponse> getByDate(Long userId, LocalDate date) {
        LocalDateTime from = date.atStartOfDay();
        LocalDateTime to   = date.plusDays(1).atStartOfDay();
        return repo.findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(userId, from, to)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    /**
     * Записи в диапазоне дат (включительно, по createdAt).
     */
    @Transactional(readOnly = true)
    public List<JournalEntryResponse> getByDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        LocalDateTime from = startDate.atStartOfDay();
        LocalDateTime to   = endDate.plusDays(1).atStartOfDay();
        return repo.findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(userId, from, to)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    /**
     * Только избранные записи.
     */
    @Transactional(readOnly = true)
    public List<JournalEntryResponse> getFavorites(Long userId) {
        return repo.findByUserIdAndIsFavoriteTrueOrderByCreatedAtDesc(userId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────
    // UPDATE — полное (PUT)
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public JournalEntryResponse update(Long userId, Long id, JournalEntryRequest req) {
        JournalEntry entry = findOwned(userId, id);

        entry.setTitle(req.getTitle());
        entry.setContent(req.getContent());
        if (req.getMoodScore() != null)  entry.setMoodScore(req.getMoodScore());
        if (req.getTags() != null)       entry.setTags(req.getTags());
        if (req.getIsFavorite() != null) entry.setIsFavorite(req.getIsFavorite());

        entry = repo.save(entry);
        log.info("Journal entry {} fully updated for user {}", id, userId);
        return toResponse(entry);
    }

    // ─────────────────────────────────────────────────────────────
    // PATCH — частичное обновление / auto-save (PATCH)
    // Обновляет только те поля, которые не null в запросе.
    // Вызывается фронтендом с дебаунсом (2-3 сек после ввода).
    // Задача WRITE_NOTE НЕ триггерится повторно.
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public JournalEntryResponse patch(Long userId, Long id, JournalEntryRequest req) {
        JournalEntry entry = findOwned(userId, id);

        if (req.getTitle() != null)      entry.setTitle(req.getTitle());
        if (req.getContent() != null)    entry.setContent(req.getContent());
        if (req.getMoodScore() != null)  entry.setMoodScore(req.getMoodScore());
        if (req.getTags() != null)       entry.setTags(req.getTags());
        if (req.getIsFavorite() != null) entry.setIsFavorite(req.getIsFavorite());

        entry = repo.save(entry);
        log.debug("Journal entry {} auto-saved for user {}", id, userId);
        return toResponse(entry);
    }

    // ─────────────────────────────────────────────────────────────
    // DELETE
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public void delete(Long userId, Long id) {
        JournalEntry entry = findOwned(userId, id);
        repo.delete(entry);
        log.info("Journal entry {} deleted for user {}", id, userId);
    }

    // ─────────────────────────────────────────────────────────────
    // INTERNAL HELPERS
    // ─────────────────────────────────────────────────────────────

    public JournalEntry findOwned(Long userId, Long id) {
        return repo.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Journal entry " + id + " not found or access denied"));
    }

    public JournalEntryResponse toResponse(JournalEntry e) {
        return JournalEntryResponse.builder()
                .id(e.getId())
                .userId(e.getUserId())
                .title(e.getTitle())
                .content(e.getContent())
                .moodScore(e.getMoodScore())
                .moodEmoji(JournalEntry.moodEmoji(e.getMoodScore()))
                .tags(e.getTags())
                .isFavorite(e.getIsFavorite())
                .wordCount(e.getWordCount())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
