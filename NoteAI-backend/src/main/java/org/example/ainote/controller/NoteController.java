package org.example.ainote.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ainote.dto.NoteDTO;
import org.example.ainote.dto.validation.OnUpdate;
import org.example.ainote.entity.Note;
import org.example.ainote.mapper.NoteMapper;
import org.example.ainote.service.NoteService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST Controller for Notes
 * ✅ NO CHANGES NEEDED - Already correctly extracts userId from request
 */
@RestController
@RequestMapping("/api/v1/notes")
@RequiredArgsConstructor
@Validated
@Slf4j
public class NoteController {

    private final NoteService noteService;
    private final NoteMapper noteMapper;

    /**
     * Get all notes for current user
     * GET /api/v1/notes
     */
    @GetMapping
    public List<NoteDTO> getNotesForCurrentUser(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        log.info("GET /notes - User {} fetching notes", userId);

        return noteService.getAllByUserId(userId)
                .stream()
                .map(noteMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Get note by ID
     * GET /api/v1/notes/{id}
     */
    @GetMapping("/{id}")
    public NoteDTO getNoteById(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("userId");
        log.info("GET /notes/{} - User {}", id, userId);

        return noteMapper.toDto(noteService.getNote(userId, id));
    }

    /**
     * Delete note
     * DELETE /api/v1/notes/{id}
     */
    @DeleteMapping("/{id}")
    public void deleteNoteById(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("userId");
        log.info("DELETE /notes/{} - User {}", id, userId);

        noteService.deleteNote(userId, id);
    }

    /**
     * Update note
     * PUT /api/v1/notes
     */
    @PutMapping
    public NoteDTO updateNote(HttpServletRequest request,
                              @Validated(OnUpdate.class) @RequestBody NoteDTO note) {
        Long userId = (Long) request.getAttribute("userId");
        log.info("PUT /notes - User {} updating note {}", userId, note.getId());

        Note noteEntity = noteMapper.toEntity(note);
        Note updatedNote = noteService.updateNote(userId, noteEntity);
        return noteMapper.toDto(updatedNote);
    }
}