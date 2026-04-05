package org.example.ainote.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ainote.dto.NoteDTO;
import org.example.ainote.dto.validation.OnCreate;
import org.example.ainote.entity.Note;
import org.example.ainote.mapper.NoteMapper;
import org.example.ainote.service.NoteService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * User Controller - Create notes for users
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Validated
@Slf4j
public class UserController {

    private final NoteService noteService;
    private final NoteMapper noteMapper;

    /**
     * Create note for current user
     * POST /api/v1/users/notes
     */
    @PostMapping("/notes")
    public NoteDTO createNote(HttpServletRequest request,
                              @Validated(OnCreate.class) @RequestBody NoteDTO noteDTO) {
        Long userId = (Long) request.getAttribute("userId");
        log.info("POST /users/notes - User {} creating note: {}", userId, noteDTO.getTitle());

        Note createdNote = noteService.createNoteForUser(
                userId,
                noteDTO.getTitle(),
                noteDTO.getContent()
        );

        return noteMapper.toDto(createdNote);
    }
}