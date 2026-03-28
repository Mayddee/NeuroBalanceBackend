package org.example.ainote.controller;

import lombok.RequiredArgsConstructor;
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

@RestController
@RequestMapping("/api/v1/notes")
@RequiredArgsConstructor
@Validated
public class NoteController {

    private final NoteService noteService;
    private final NoteMapper noteMapper;

    @GetMapping
    public List<NoteDTO> getNotesForCurrentUser(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return noteService.getAllByUserId(userId)
                .stream()
                .map(noteMapper::toDto)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public NoteDTO getNoteById(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("userId");
        return noteMapper.toDto(noteService.getNote(userId, id));
    }

    @DeleteMapping("/{id}")
    public void deleteNoteById(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("userId");
        noteService.deleteNote(userId, id);
    }

    @PutMapping
    public NoteDTO updateNote(HttpServletRequest request,
                              @Validated(OnUpdate.class) @RequestBody NoteDTO note) {
        Long userId = (Long) request.getAttribute("userId");
        Note noteEntity = noteMapper.toEntity(note);
        Note updatedNote = noteService.updateNote(userId, noteEntity);
        return noteMapper.toDto(updatedNote);
    }
}