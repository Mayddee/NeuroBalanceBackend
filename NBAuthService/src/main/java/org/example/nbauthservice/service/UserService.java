package org.example.nbauthservice.service;

import lombok.RequiredArgsConstructor;
import org.example.nbauthservice.dto.RegisterRequest;
import org.example.nbauthservice.dto.UserShortDto;
import org.example.nbauthservice.entity.Role;
import org.example.nbauthservice.entity.User;
import org.example.nbauthservice.exception.EntityNotFoundException;
import org.example.nbauthservice.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;
    private final SmsVerificationService smsVerificationService;

    @Transactional(readOnly = true)
    public User getById(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("User not found"));
    }

    @Transactional(readOnly = true)
    public User getByUsername(String username) {
        return userRepository.findByUsername(username).orElseThrow(() -> new EntityNotFoundException("User not found!"));
    }

    @Transactional(readOnly = true)
    public User getByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found by email"));
    }

    @Transactional
    public User update(User user){
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    @Transactional
    public User createFromRegister(RegisterRequest request) {
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setUsername(request.getUsername());
        user.setPassword(request.getPassword());
        return create(user);
    }


@Transactional
public User create(User user) {
    // Если юзер уже есть, но не verified → не создаём заново!
    Optional<User> existing = userRepository.findByEmail(user.getEmail());
    if (existing.isPresent()) {
        User found = existing.get();
        if (!found.isEmailVerified()) {
            emailVerificationService.sendVerificationEmail(found);

            // consequence: токен будет перевыпущен и письмо отправлено ещё раз
            // пользователь НЕ дублируется в БД
            return found;
        }
        throw new IllegalArgumentException("User already registered and verified.");

        // consequence: если уже verified — новый register вернет ошибку 400/500
    }

    // Иначе — создаём с дефолт ролью
    user.setId(null);
    user.setPassword(passwordEncoder.encode(user.getPassword()));
    user.setRoles(Set.of(Role.USER));
    user.setEmailVerified(false);
    user.setPhoneVerified(false);

    User saved = userRepository.save(user);
    emailVerificationService.sendVerificationEmail(saved);
    return saved;
}



    @Transactional(readOnly = true)
    public boolean isNoteOwner(Long noteId, Long userId) {
        return userRepository.isNoteOwner(noteId, userId);
    }

    @Transactional
    public void delete(Long userId) {
        userRepository.deleteById(userId);
    }

    @Transactional
    public void verifyUserManually(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setEmailVerified(true);
        user.setPhoneVerified(true);
        userRepository.save(user);

        System.out.println(">>> User " + username + " manually verified");
    }

    @Transactional(readOnly = true)
    public List<UserShortDto> searchUsers(String query) {
        return userRepository
                .findTop20ByUsernameContainingIgnoreCase(query)
                .stream()
                .map(u -> {
                    UserShortDto dto = new UserShortDto();
                    dto.setId(u.getId());
                    dto.setUsername(u.getUsername());
                    dto.setName(u.getName());
                    return dto;
                })
                .toList();
    }




}
