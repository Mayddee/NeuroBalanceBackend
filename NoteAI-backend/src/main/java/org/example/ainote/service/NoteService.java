package org.example.ainote.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ainote.entity.Note;
import org.example.ainote.entity.NoteUser;
import org.example.ainote.exception.EntityNotFoundException;
import org.example.ainote.repository.NoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for managing notes
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NoteService {

    private final NoteRepository noteRepository;
    private final NoteUserService noteUserService;

    @Transactional(readOnly = true)
    public Note getById(Long id) {
        return noteRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Note not found."));
    }

    @Transactional
    public Note createNoteForUser(Long userId, String title, String content) {
        log.info("Creating note for user {}", userId);

        NoteUser user = noteUserService.getOrCreate(userId);

        Note note = new Note();
        note.setTitle(title);
        note.setContent(content);
        noteRepository.save(note);

        user.getNotes().add(note);
        noteUserService.update(user);

        log.info("Note created with ID: {}", note.getId());
        return note;
    }

    @Transactional(readOnly = true)
    public List<Note> getAllByUserId(Long userId) {
        log.debug("Fetching all notes for user {}", userId);

        return noteRepository.findAllByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Notes not found."));
    }

    @Transactional(readOnly = true)
    public Note getNote(Long userId, Long noteId) {
        log.debug("Fetching note {} for user {}", noteId, userId);

        if (!noteUserService.isNoteOwner(noteId, userId)) {
            throw new EntityNotFoundException("Note not found or access denied!");
        }
        return getById(noteId);
    }

    @Transactional
    public void deleteNote(Long userId, Long noteId) {
        log.info("Deleting note {} for user {}", noteId, userId);

        if (!noteUserService.isNoteOwner(noteId, userId)) {
            throw new EntityNotFoundException("You do not have permission to delete this note");
        }

        NoteUser user = noteUserService.getById(userId);
        Note note = getById(noteId);

        user.getNotes().remove(note);
        noteUserService.update(user);
        noteRepository.delete(note);

        log.info("Note {} deleted successfully", noteId);
    }

    @Transactional
    public Note updateNote(Long userId, Note updatedNote) {
        log.info("Updating note {} for user {}", updatedNote.getId(), userId);

        if (!noteUserService.isNoteOwner(updatedNote.getId(), userId)) {
            throw new EntityNotFoundException("Access denied to update note");
        }

        Note existingNote = noteRepository.findById(updatedNote.getId())
                .orElseThrow(() -> new EntityNotFoundException("Note does not exist!"));

        existingNote.setTitle(updatedNote.getTitle());
        existingNote.setContent(updatedNote.getContent());

        Note saved = noteRepository.save(existingNote);
        log.info("Note {} updated successfully", updatedNote.getId());

        return saved;
    }
}