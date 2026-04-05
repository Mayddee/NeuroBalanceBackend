package org.example.nbauthservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.example.nbauthservice.dto.OnboardingDTO;
import org.example.nbauthservice.dto.UserDTO;
import org.example.nbauthservice.dto.UserShortDto;
import org.example.nbauthservice.dto.validation.OnUpdate;
import org.example.nbauthservice.entity.User;
import org.example.nbauthservice.entity.UserOnboarding;
import org.example.nbauthservice.mapper.OnboardingMapper;
import org.example.nbauthservice.mapper.UserMapper;
import org.example.nbauthservice.repository.UserOnboardingRepository;
import org.example.nbauthservice.security.JwtEntity;
import org.example.nbauthservice.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Validated
public class UserController {
    private final UserService userService;
    private final UserMapper userMapper;
    private final UserOnboardingRepository onboardingRepository;
    private final OnboardingMapper onboardingMapper;

    /**
     * Получить userId из JWT токена
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof JwtEntity) {
            JwtEntity jwtEntity = (JwtEntity) authentication.getPrincipal();
            return jwtEntity.getId();
        }
        throw new RuntimeException("User not authenticated");
    }

    /**
     * ✅ НОВЫЙ ENDPOINT: Получить полную информацию о текущем пользователе
     * GET /api/v1/users/me
     */
    @GetMapping("/me")
    @Operation(summary = "Get current user",
            description = "Get full information about current user including onboarding data. Requires JWT authentication.")
    public UserDTO getCurrentUser() {
        Long userId = getCurrentUserId();

        User user = userService.getById(userId);
        UserDTO userDTO = userMapper.toDto(user);

        // ✅ Добавить данные онбординга, если есть
        Optional<UserOnboarding> onboarding = onboardingRepository.findByUserId(userId);
        if (onboarding.isPresent()) {
            OnboardingDTO onboardingDTO = onboardingMapper.toDto(onboarding.get());
            userDTO.setOnboarding(onboardingDTO);
        }

        return userDTO;
    }

    @PutMapping
    public UserDTO update(@Validated(OnUpdate.class) @RequestBody UserDTO userDTO) {
        User user = userMapper.toEntity(userDTO);
        user = userService.update(user);
        return userMapper.toDto(user);
    }

    @GetMapping("/search")
    public List<UserShortDto> search(@RequestParam String q) {
        return userService.searchUsers(q);
    }

    @GetMapping("/{id}")
    public UserDTO getById(@PathVariable Long id) {
        User user = userService.getById(id);
        UserDTO userDTO = userMapper.toDto(user);

        Optional<UserOnboarding> onboarding = onboardingRepository.findByUserId(id);
        if (onboarding.isPresent()) {
            OnboardingDTO onboardingDTO = onboardingMapper.toDto(onboarding.get());
            userDTO.setOnboarding(onboardingDTO);
        }

        return userDTO;
    }

    @DeleteMapping("/{id}")
    public void deleteById(@PathVariable Long id) {
        userService.delete(id);
    }
}