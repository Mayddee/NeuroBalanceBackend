package org.example.nbcheckinservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nbcheckinservice.dto.CharacterResponse;
import org.example.nbcheckinservice.dto.CharacterSelectionRequest;
import org.example.nbcheckinservice.entity.UserCharacter;
import org.example.nbcheckinservice.repository.UserCharacterRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service for managing user characters/pets
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserCharacterService {

    private final UserCharacterRepository characterRepository;

    /**
     * Get or create character for user (default: MOA, Level 1)
     */
    @Transactional
    public UserCharacter getOrCreateCharacter(Long userId) {
        return characterRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultCharacter(userId));
    }

    /**
     * Create default character (MOA)
     */
    private UserCharacter createDefaultCharacter(Long userId) {
        log.info("Creating default character (MOA) for user {}", userId);

        UserCharacter character = UserCharacter.builder()
                .userId(userId)
                .characterType(UserCharacter.CharacterType.MOA)
                .characterName("Моа")
                .currentLevel(1)
                .totalXp(0)
                .xpForNextLevel(500)
                .happinessLevel(50)
                .energyLevel(100)
                .build();

        return characterRepository.save(character);
    }

    /**
     * Add XP to character and handle level ups
     */
    @Transactional
    public CharacterResponse addXp(Long userId, int xpToAdd) {
        log.info("Adding {} XP to character for user {}", xpToAdd, userId);

        UserCharacter character = getOrCreateCharacter(userId);
        boolean leveledUp = character.addXp(xpToAdd);

        if (leveledUp) {
            log.info("🎉 Character leveled up to level {}!", character.getCurrentLevel());
        }

        UserCharacter savedCharacter = characterRepository.save(character);
        return buildCharacterResponse(savedCharacter, leveledUp);
    }

    /**
     * Update character happiness based on wellness score
     */
    @Transactional
    public void updateHappiness(Long userId, double wellnessScore) {
        UserCharacter character = getOrCreateCharacter(userId);
        character.updateHappiness(wellnessScore);
        characterRepository.save(character);

        log.debug("Updated character happiness for user {}: {} (wellness: {})",
                userId, character.getHappinessLevel(), wellnessScore);
    }

    /**
     * Get character response
     */
    @Transactional(readOnly = true)
    public CharacterResponse getCharacterResponse(Long userId) {
        UserCharacter character = getOrCreateCharacter(userId);
        return buildCharacterResponse(character, false);
    }

    /**
     * Change character type (user can choose different pet)
     */
    @Transactional
    public CharacterResponse changeCharacterType(Long userId, UserCharacter.CharacterType newType) {
        log.info("Changing character type for user {} to {}", userId, newType);

        UserCharacter character = getOrCreateCharacter(userId);
        character.setCharacterType(newType);
        character.setCharacterName(newType.getDisplayName());

        UserCharacter savedCharacter = characterRepository.save(character);
        return buildCharacterResponse(savedCharacter, false);
    }

    // ========== HELPER METHODS ==========

    private CharacterResponse buildCharacterResponse(UserCharacter character, boolean justLeveledUp) {
        return CharacterResponse.builder()
                .userId(character.getUserId())
                .characterType(character.getCharacterType().name())
                .characterName(character.getCharacterName())
                .characterEmoji(character.getCharacterEmoji())
                .currentLevel(character.getCurrentLevel())
                .totalXp(character.getTotalXp())
                .xpForNextLevel(character.getXpForNextLevel())
                .levelProgress(character.getLevelProgressPercentage())
                .happinessLevel(character.getHappinessLevel())
                .energyLevel(character.getEnergyLevel())
                .description(character.getCharacterDescription())
                .justLeveledUp(justLeveledUp)
                .isMaxLevel(character.getCurrentLevel() >= 5)
                .build();
    }

    /**
     * Increase character happiness (for playing games, etc)
     */
    @Transactional
    public void increaseHappiness(Long userId, int amount) {
        UserCharacter character = getOrCreateCharacter(userId);

        int newHappiness = Math.min(100, character.getHappinessLevel() + amount);
        character.setHappinessLevel(newHappiness);
        character.setLastInteractionAt(LocalDateTime.now());

        characterRepository.save(character);

        log.debug("Increased character happiness for user {} by {}: {} → {}",
                userId, amount, character.getHappinessLevel() - amount, newHappiness);
    }

    /**
     * Feed character (increases happiness and energy)
     */
    @Transactional
    public CharacterResponse feedCharacter(Long userId) {
        UserCharacter character = getOrCreateCharacter(userId);

        // Increase happiness and energy
        int newHappiness = Math.min(100, character.getHappinessLevel() + 10);
        int newEnergy = Math.min(100, character.getEnergyLevel() + 15);

        character.setHappinessLevel(newHappiness);
        character.setEnergyLevel(newEnergy);
        character.setLastInteractionAt(LocalDateTime.now());

        UserCharacter saved = characterRepository.save(character);

        log.info("Character fed for user {}: happiness +10, energy +15", userId);

        return buildCharacterResponse(saved, false);
    }

    /**
     * Play with character (increases happiness, decreases energy slightly)
     */
    @Transactional
    public CharacterResponse playWithCharacter(Long userId) {
        UserCharacter character = getOrCreateCharacter(userId);

        // Increase happiness, slightly decrease energy
        int newHappiness = Math.min(100, character.getHappinessLevel() + 15);
        int newEnergy = Math.max(0, character.getEnergyLevel() - 5);

        character.setHappinessLevel(newHappiness);
        character.setEnergyLevel(newEnergy);
        character.setLastInteractionAt(LocalDateTime.now());

        UserCharacter saved = characterRepository.save(character);

        log.info("Played with character for user {}: happiness +15, energy -5", userId);

        return buildCharacterResponse(saved, false);
    }

    /**
     * Выбор персонажа пользователем (POST запрос)
     */
//    @Transactional
//    public CharacterResponse selectInitialCharacter(Long userId, CharacterSelectionRequest request) {
//        log.info("User {} is selecting initial character: {}", userId, request.getCharacterType());
//        UserCharacter character = characterRepository.findByUserId(userId)
//                .orElse(new UserCharacter());
//
//        character.setUserId(userId);
//
//        // Парсим тип персонажа из строки
//        UserCharacter.CharacterType type;
//        try {
//            type = UserCharacter.CharacterType.valueOf(request.getCharacterType().toUpperCase());
//        } catch (IllegalArgumentException e) {
//            throw new IllegalArgumentException("Invalid character type. Allowed: MOA, BUL, PIKO, SPARKY");
//        }
//
//        // Определяем имя (если пользователь ввел свое - берем его, иначе стандартное)
//        String name = (request.getCustomName() != null && !request.getCustomName().trim().isEmpty())
//                ? request.getCustomName()
//                : type.getDisplayName();
//
//        // Создаем персонажа
//        UserCharacter character = UserCharacter.builder()
//                .userId(userId)
//                .characterType(type)
//                .characterName(name)
//                .currentLevel(1)
//                .totalXp(0)
//                .xpForNextLevel(500)
//                .happinessLevel(50)
//                .energyLevel(100)
//                .build();
//
//        UserCharacter savedCharacter = characterRepository.save(character);
//
//        return buildCharacterResponse(savedCharacter, false);
//    }
    @Transactional
    public CharacterResponse selectInitialCharacter(Long userId, CharacterSelectionRequest request) {
        log.info("User {} selecting character {}", userId, request.getCharacterType());

        if (characterRepository.findByUserId(userId).isPresent()) {
            throw new IllegalStateException("User already has a character!");
        }

        UserCharacter.CharacterType type;
        try {
            type = UserCharacter.CharacterType.valueOf(request.getCharacterType().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid character type: " + request.getCharacterType());
        }

        String name = (request.getCustomName() != null && !request.getCustomName().trim().isEmpty())
                ? request.getCustomName()
                : type.getDisplayName();

        UserCharacter newCharacter = UserCharacter.builder()
                .userId(userId)
                .characterType(type)
                .characterName(name)
                .currentLevel(1)
                .totalXp(0)
                .xpForNextLevel(500)
                .happinessLevel(50)
                .energyLevel(100)
                .build();

        UserCharacter savedCharacter = characterRepository.save(newCharacter);
        return buildCharacterResponse(savedCharacter, false);
    }
}