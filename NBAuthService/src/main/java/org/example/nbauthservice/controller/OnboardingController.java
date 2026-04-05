package org.example.nbauthservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.example.nbauthservice.dto.OnboardingDTO;
import org.example.nbauthservice.dto.validation.OnCreate;
import org.example.nbauthservice.security.JwtEntity;
import org.example.nbauthservice.service.OnboardingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/onboarding")
@RequiredArgsConstructor
@Validated
@Tag(name = "Onboarding", description = "User onboarding data management")
public class OnboardingController {

    private final OnboardingService onboardingService;

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
     * Создать или полностью обновить онбординг текущего пользователя
     */
    @PostMapping
    @Operation(summary = "Create or update onboarding",
            description = "Complete user onboarding with all required fields. Requires JWT authentication.")
    public ResponseEntity<OnboardingDTO> createOrUpdate(
            @Validated(OnCreate.class) @RequestBody OnboardingDTO dto) {

        Long userId = getCurrentUserId();

        OnboardingDTO result = onboardingService.createOrUpdate(userId, dto);
        return ResponseEntity.ok(result);
    }

    /**
     * Получить онбординг текущего пользователя
     */
    @GetMapping
    @Operation(summary = "Get onboarding data",
            description = "Retrieve current user's onboarding data. Requires JWT authentication.")
    public ResponseEntity<OnboardingDTO> getMyOnboarding() {
        Long userId = getCurrentUserId();

        try {
            OnboardingDTO result = onboardingService.getByUserId(userId);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Проверить завершен ли онбординг
     */
    @GetMapping("/status")
    @Operation(summary = "Check onboarding status",
            description = "Check if current user has completed onboarding. Requires JWT authentication.")
    public ResponseEntity<Map<String, Boolean>> checkStatus() {
        Long userId = getCurrentUserId();

        boolean isCompleted = onboardingService.isCompleted(userId);
        return ResponseEntity.ok(Map.of(
                "isOnboarded", isCompleted,
                "isCompleted", isCompleted
        ));
    }

    /**
     * Частично обновить онбординг
     */
    @PatchMapping
    @Operation(summary = "Partially update onboarding",
            description = "Update only specified fields of onboarding data. Requires JWT authentication.")
    public ResponseEntity<OnboardingDTO> updatePartial(
            @RequestBody OnboardingDTO dto) {

        Long userId = getCurrentUserId();

        try {
            OnboardingDTO result = onboardingService.updatePartial(userId, dto);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Удалить онбординг
     */
    @DeleteMapping
    @Operation(summary = "Delete onboarding",
            description = "Delete current user's onboarding data. Requires JWT authentication.")
    public ResponseEntity<Map<String, String>> delete() {
        Long userId = getCurrentUserId();

        try {
            onboardingService.delete(userId);
            return ResponseEntity.ok(Map.of(
                    "message", "Onboarding deleted successfully"
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Получить онбординг конкретного пользователя (для админов)
     */
    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user onboarding (admin)",
            description = "Retrieve onboarding data for specific user. Admin only.")
    public ResponseEntity<OnboardingDTO> getUserOnboarding(@PathVariable Long userId) {
        try {
            OnboardingDTO result = onboardingService.getByUserId(userId);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}