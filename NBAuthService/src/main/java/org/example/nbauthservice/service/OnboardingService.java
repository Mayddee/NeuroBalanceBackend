package org.example.nbauthservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nbauthservice.dto.OnboardingDTO;
import org.example.nbauthservice.entity.User;
import org.example.nbauthservice.entity.UserOnboarding;
import org.example.nbauthservice.mapper.OnboardingMapper;
import org.example.nbauthservice.repository.UserOnboardingRepository;
import org.example.nbauthservice.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OnboardingService {

    private final UserOnboardingRepository onboardingRepository;
    private final UserRepository userRepository;
    private final OnboardingMapper onboardingMapper;

    /**
     * Создать или обновить онбординг для пользователя
     */
    @Transactional
    public OnboardingDTO createOrUpdate(Long userId, OnboardingDTO dto) {
        log.info("Creating/updating onboarding for user {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Проверить согласие на сбор данных
        if (dto.getDataConsent() == null || !dto.getDataConsent()) {
            throw new IllegalArgumentException("User must consent to data collection");
        }

        UserOnboarding onboarding = onboardingRepository.findByUserId(userId)
                .orElse(UserOnboarding.builder()
                        .user(user)
                        .build());

        // Обновить данные
        onboarding.setSex(dto.getSex());
        onboarding.setHeightCm(dto.getHeightCm());
        onboarding.setWeightKg(dto.getWeightKg());
        onboarding.setBirthDate(dto.getBirthDate());
        onboarding.setCharacterId(dto.getCharacterId());
        onboarding.setDataConsent(dto.getDataConsent());
        onboarding.setCompleted(true);

        onboarding = onboardingRepository.save(onboarding);

        // Обновить флаг onboarded у пользователя
        user.setOnboarded(true);
        userRepository.save(user);

        log.info("Onboarding completed for user {}", userId);
        return onboardingMapper.toDto(onboarding);
    }

    /**
     * Получить онбординг пользователя
     */
    @Transactional(readOnly = true)
    public OnboardingDTO getByUserId(Long userId) {
        log.info("Getting onboarding for user {}", userId);

        UserOnboarding onboarding = onboardingRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Onboarding not found for user " + userId));

        return onboardingMapper.toDto(onboarding);
    }

    /**
     * Проверить завершен ли онбординг
     */
    @Transactional(readOnly = true)
    public boolean isCompleted(Long userId) {
        return onboardingRepository.findByUserId(userId)
                .map(UserOnboarding::isCompleted)
                .orElse(false);
    }

    /**
     * Удалить онбординг пользователя
     */
    @Transactional
    public void delete(Long userId) {
        log.info("Deleting onboarding for user {}", userId);

        UserOnboarding onboarding = onboardingRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Onboarding not found for user " + userId));

        onboardingRepository.delete(onboarding);

        // Обновить флаг у пользователя
        User user = userRepository.findById(userId).orElseThrow();
        user.setOnboarded(false);
        userRepository.save(user);

        log.info("Onboarding deleted for user {}", userId);
    }

    /**
     * Частичное обновление онбординга
     */
    @Transactional
    public OnboardingDTO updatePartial(Long userId, OnboardingDTO dto) {
        log.info("Partially updating onboarding for user {}", userId);

        UserOnboarding onboarding = onboardingRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Onboarding not found for user " + userId));

        // Обновить только непустые поля
        if (dto.getSex() != null) onboarding.setSex(dto.getSex());
        if (dto.getHeightCm() != null) onboarding.setHeightCm(dto.getHeightCm());
        if (dto.getWeightKg() != null) onboarding.setWeightKg(dto.getWeightKg());
        if (dto.getBirthDate() != null) onboarding.setBirthDate(dto.getBirthDate());
        if (dto.getCharacterId() != null) onboarding.setCharacterId(dto.getCharacterId());
        if (dto.getDataConsent() != null) onboarding.setDataConsent(dto.getDataConsent());

        onboarding = onboardingRepository.save(onboarding);

        log.info("Onboarding partially updated for user {}", userId);
        return onboardingMapper.toDto(onboarding);
    }
}