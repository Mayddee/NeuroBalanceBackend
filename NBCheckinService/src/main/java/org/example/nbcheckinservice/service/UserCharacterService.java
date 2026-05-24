package org.example.nbcheckinservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nbcheckinservice.dto.CharacterResponse;
import org.example.nbcheckinservice.dto.CharacterSelectionRequest;
import org.example.nbcheckinservice.entity.UserCharacter;
import org.example.nbcheckinservice.kafka.KafkaProducerService;
import org.example.nbcheckinservice.repository.UserCharacterRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j
public class UserCharacterService {

    private final UserCharacterRepository characterRepository;
    private final StreakService streakService;
    private final KafkaProducerService kafkaProducerService;

    public UserCharacterService(UserCharacterRepository characterRepository,
                                @Lazy StreakService streakService,
                                KafkaProducerService kafkaProducerService) {
        this.characterRepository = characterRepository;
        this.streakService = streakService;
        this.kafkaProducerService = kafkaProducerService;
    }

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
     * Add XP to character with a streak gate on level-up.
     *
     * Dynamic XP thresholds (formula: 500 × currentLevel²):
     *   Lv1→2:  500 total XP
     *   Lv2→3: 2000 total XP
     *   Lv3→4: 4500 total XP
     *   Lv4→5: 8000 total XP
     *
     * Streak requirements per level transition:
     *   Lv1→2: 0 days (free first level-up)
     *   Lv2→3: 7 days
     *   Lv3→4: 14 days
     *   Lv4→5: 21 days
     */
    @Transactional
    public CharacterResponse addXp(Long userId, int xpToAdd) {
        if (xpToAdd < 0) xpToAdd = 0;
        log.debug("Adding {} XP to character for user {}", xpToAdd, userId);

        UserCharacter character = getOrCreateCharacter(userId);
        // Enforce dynamic thresholds regardless of what the entity stored
        recalibrateXpThreshold(character);
        int oldLevel = character.getCurrentLevel();

        if (xpToAdd > 0 && character.getCurrentLevel() < 5) {
            int projectedXp = character.getTotalXp() + xpToAdd;
            if (projectedXp >= character.getXpForNextLevel()) {
                int requiredStreak = requiredStreakForLevel(character.getCurrentLevel());
                if (requiredStreak > 0) {
                    int currentStreak = streakService.getOrCreateStreak(userId).getCurrentStreak();
                    if (currentStreak < requiredStreak) {
                        int capped = Math.max(0, character.getXpForNextLevel() - character.getTotalXp() - 1);
                        log.info("Level-up gated for user {}: streak {}/{} required, XP capped at {}",
                                userId, currentStreak, requiredStreak, capped);
                        xpToAdd = capped;
                    }
                }
            }
        }

        boolean leveledUp = character.addXp(xpToAdd);

        if (leveledUp) {
            // Override the entity's hardcoded threshold with the dynamic value
            recalibrateXpThreshold(character);
            log.info("Character leveled up: user={}, {}→{}, totalXp={}, nextThreshold={}",
                    userId, oldLevel, character.getCurrentLevel(), character.getTotalXp(), character.getXpForNextLevel());
            kafkaProducerService.publishLevelUp(userId, oldLevel, character.getCurrentLevel(),
                    character.getCharacterType().name(), character.getCharacterEmoji(), character.getTotalXp());
        }

        characterRepository.save(character);
        return buildCharacterResponse(character, leveledUp);
    }

    /**
     * Called by CharacterProgressionConsumer after every game or check-in event.
     * Unlocks a pending level-up when:
     *   - totalXp is at threshold-1 (capped by streak gate), AND
     *   - the streak requirement for this level is now satisfied.
     */
    @Transactional
    public void checkAndAutoLevelUp(Long userId) {
        UserCharacter character = getOrCreateCharacter(userId);
        if (character.getCurrentLevel() >= 5) return;

        recalibrateXpThreshold(character);

        int requiredStreak = requiredStreakForLevel(character.getCurrentLevel());
        if (requiredStreak == 0) return;

        boolean isPendingLevelUp = character.getTotalXp() == character.getXpForNextLevel() - 1;
        if (!isPendingLevelUp) return;

        int currentStreak = streakService.getOrCreateStreak(userId).getCurrentStreak();
        if (currentStreak < requiredStreak) return;

        int oldLevel = character.getCurrentLevel();
        int threshold = character.getXpForNextLevel();

        character.setTotalXp(threshold);
        character.setCurrentLevel(oldLevel + 1);
        character.setXpForNextLevel(dynamicXpThreshold(oldLevel + 1));
        character.setHappinessLevel(Math.min(100, character.getHappinessLevel() + 10));
        character.setEnergyLevel(100);

        characterRepository.save(character);

        log.info("Auto level-up unlocked for user {}: {}→{} (streak={}/{})",
                userId, oldLevel, character.getCurrentLevel(), currentStreak, requiredStreak);
        kafkaProducerService.publishLevelUp(userId, oldLevel, character.getCurrentLevel(),
                character.getCharacterType().name(), character.getCharacterEmoji(), character.getTotalXp());
    }

    // Minimum check-in streak needed to level up FROM currentLevel to currentLevel+1
    private int requiredStreakForLevel(int currentLevel) {
        return switch (currentLevel) {
            case 1 -> 0;   // Lv1→2: free
            case 2 -> 7;   // Lv2→3: 7-day streak
            case 3 -> 14;  // Lv3→4: 14-day streak
            case 4 -> 21;  // Lv4→5: 21-day streak
            default -> 0;
        };
    }

    /**
     * Dynamic XP threshold (cumulative total XP to reach next level).
     * Formula: 500 × currentLevel²
     *   Lv1→2:  500 XP  (500 × 1²)
     *   Lv2→3: 2000 XP  (500 × 2²)
     *   Lv3→4: 4500 XP  (500 × 3²)
     *   Lv4→5: 8000 XP  (500 × 4²)
     */
    private int dynamicXpThreshold(int currentLevel) {
        if (currentLevel >= 5) return 0;
        return 500 * currentLevel * currentLevel;
    }

    private void recalibrateXpThreshold(UserCharacter character) {
        if (character.getCurrentLevel() < 5) {
            character.setXpForNextLevel(dynamicXpThreshold(character.getCurrentLevel()));
        }
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

    @Transactional(readOnly = true)
    public CharacterResponse getCharacterResponse(Long userId) {
        UserCharacter character = getOrCreateCharacter(userId);
        recalibrateXpThreshold(character);
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