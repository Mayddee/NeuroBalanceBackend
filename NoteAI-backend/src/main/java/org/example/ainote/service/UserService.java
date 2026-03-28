package org.example.ainote.service;

import lombok.RequiredArgsConstructor;
import org.example.ainote.entity.User;
import org.example.ainote.exception.EntityNotFoundException;
import org.example.ainote.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }

    @Transactional(readOnly = true)
    public User getByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found!"));
    }

    @Transactional
    public User update(User user) {
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public boolean isNoteOwner(Long noteId, Long userId) {
        return userRepository.isNoteOwner(noteId, userId);
    }

    @Transactional
    public void delete(Long userId) {
        userRepository.deleteById(userId);
    }

    /**
     * Создание пользователя (вызывается только при первом обращении из NBAuthService)
     */
    @Transactional
    public User createIfNotExists(Long userId, String username, String email, String name) {
        return userRepository.findById(userId).orElseGet(() -> {
            User newUser = new User();
            newUser.setId(userId);
            newUser.setUsername(username);
            newUser.setEmail(email);
            newUser.setName(name);
            return userRepository.save(newUser);
        });
    }
}
