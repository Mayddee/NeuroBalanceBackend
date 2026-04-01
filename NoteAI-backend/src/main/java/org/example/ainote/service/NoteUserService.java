package org.example.ainote.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ainote.entity.NoteUser;
import org.example.ainote.exception.EntityNotFoundException;
import org.example.ainote.repository.NoteUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing NoteUser (local user data for note ownership)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NoteUserService {

    private final NoteUserRepository noteUserRepository;

    @Transactional(readOnly = true)
    public NoteUser getById(Long userId) {
        return noteUserRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }

    @Transactional(readOnly = true)
    public NoteUser getByUsername(String username) {
        return noteUserRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found!"));
    }

    @Transactional
    public NoteUser update(NoteUser user) {
        return noteUserRepository.save(user);
    }

    @Transactional(readOnly = true)
    public boolean isNoteOwner(Long noteId, Long userId) {
        return noteUserRepository.isNoteOwner(noteId, userId);
    }

    @Transactional
    public void delete(Long userId) {
        noteUserRepository.deleteById(userId);
    }

    /**
     * Create user if not exists (called on first request from NBAuthService)
     * This is called automatically when a user from NBAuthService first accesses notes
     */
    @Transactional
    public NoteUser createIfNotExists(Long userId, String username, String email, String name) {
        return noteUserRepository.findById(userId).orElseGet(() -> {
            log.info("Creating new NoteUser for userId: {}", userId);

            NoteUser newUser = new NoteUser();
            newUser.setUserId(userId);
            newUser.setUsername(username);
            newUser.setEmail(email);
            newUser.setName(name);

            return noteUserRepository.save(newUser);
        });
    }

    /**
     * Get or create user (ensures user exists before any note operations)
     */
    @Transactional
    public NoteUser getOrCreate(Long userId) {
        return noteUserRepository.findById(userId).orElseGet(() -> {
            log.info("Auto-creating NoteUser for userId: {}", userId);

            NoteUser newUser = new NoteUser();
            newUser.setUserId(userId);
            newUser.setUsername("user_" + userId); // Default username

            return noteUserRepository.save(newUser);
        });
    }
}