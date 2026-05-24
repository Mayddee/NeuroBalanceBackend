package org.example.nbcheckinservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nbcheckinservice.dto.RewardResponse;
import org.example.nbcheckinservice.entity.UserReward;
import org.example.nbcheckinservice.entity.UserStreak;
import org.example.nbcheckinservice.repository.DailyTaskRepository;
import org.example.nbcheckinservice.repository.GameSessionRepository;
import org.example.nbcheckinservice.repository.MoodLogRepository;
import org.example.nbcheckinservice.repository.NewGameSessionRepository;
import org.example.nbcheckinservice.repository.UserCharacterRepository;
import org.example.nbcheckinservice.repository.UserRewardRepository;
import org.example.nbcheckinservice.repository.UserStreakRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing user rewards/badges.
 * Intentionally does NOT depend on StreakService — reads streak directly from repo to avoid circular deps.
 *
 * Two reward categories:
 *  1. Streak-based  (requiredStreak > 0): unlocked when check-in streak reaches the milestone.
 *  2. Achievement   (requiredStreak = 0): unlocked by activity milestones (games played, mood logs, level, etc.).
 *
 * Auto-unlock is triggered by CharacterProgressionConsumer after every game.completed and checkin.created event.
 * StreakService also calls checkAndUnlockRewards() directly after each check-in update.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RewardService {

    private static final ZoneId ALMATY_ZONE = ZoneId.of("Asia/Almaty");
    private static final int DAILY_TASK_COUNT = 5;

    private final UserRewardRepository rewardRepository;
    private final UserStreakRepository streakRepository;
    private final GameSessionRepository gameSessionRepository;
    private final NewGameSessionRepository newGameSessionRepository;
    private final MoodLogRepository moodLogRepository;
    private final UserCharacterRepository characterRepository;
    private final DailyTaskRepository taskRepository;

    // ========== PUBLIC API ==========

    /**
     * Main entry point — checks all streak and achievement rewards and unlocks eligible ones.
     * Called automatically from CharacterProgressionConsumer (Kafka) and StreakService.
     */
    @Transactional
    public List<RewardResponse> checkAndUnlockRewards(Long userId) {
        List<UserReward> newlyUnlocked = new ArrayList<>();
        newlyUnlocked.addAll(checkStreakRewards(userId));
        newlyUnlocked.addAll(checkAchievementRewards(userId));
        return newlyUnlocked.stream().map(this::buildRewardResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RewardResponse> getAllRewards(Long userId) {
        List<UserReward> rewards = new ArrayList<>();
        for (UserReward.RewardType rewardType : UserReward.RewardType.values()) {
            rewards.add(getOrCreateRewardReadOnly(userId, rewardType));
        }
        return rewards.stream().map(this::buildRewardResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RewardResponse> getUnlockedRewards(Long userId) {
        return rewardRepository.findByUserIdAndIsUnlockedTrue(userId)
                .stream().map(this::buildRewardResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public double getActiveXpMultiplier(Long userId) {
        return rewardRepository.findByUserIdAndIsUnlockedTrueOrderByXpMultiplierDesc(userId)
                .stream()
                .findFirst()
                .map(UserReward::getXpMultiplier)
                .orElse(1.0);
    }

    // ========== STREAK REWARDS ==========

    private List<UserReward> checkStreakRewards(Long userId) {
        int currentStreak = streakRepository.findByUserId(userId)
                .map(UserStreak::getCurrentStreak)
                .orElse(0);

        List<UserReward> unlocked = new ArrayList<>();
        for (UserReward.RewardType type : UserReward.RewardType.values()) {
            if (!type.isStreakBased()) continue;
            if (currentStreak >= type.getRequiredStreak()) {
                unlocked.addAll(tryUnlock(userId, type));
            }
        }
        return unlocked;
    }

    // ========== ACHIEVEMENT REWARDS ==========

    private List<UserReward> checkAchievementRewards(Long userId) {
        List<UserReward> unlocked = new ArrayList<>();

        long totalGames = gameSessionRepository.countByUserId(userId)
                + newGameSessionRepository.countByUserId(userId);
        long totalMoodLogs = moodLogRepository.countByUserId(userId);
        int characterLevel = characterRepository.findByUserId(userId)
                .map(c -> c.getCurrentLevel())
                .orElse(1);
        boolean hasPerfectDay = hasPerfectDayToday(userId);

        if (totalGames >= 1)  unlocked.addAll(tryUnlock(userId, UserReward.RewardType.FIRST_GAME_PLAYED));
        if (totalGames >= 10) unlocked.addAll(tryUnlock(userId, UserReward.RewardType.GAME_MASTER_10));
        if (totalGames >= 50) unlocked.addAll(tryUnlock(userId, UserReward.RewardType.GAME_MASTER_50));

        if (totalMoodLogs >= 1) unlocked.addAll(tryUnlock(userId, UserReward.RewardType.FIRST_MOOD_LOG));
        if (totalMoodLogs >= 7) unlocked.addAll(tryUnlock(userId, UserReward.RewardType.MOOD_TRACKER_7));

        if (characterLevel >= 2) unlocked.addAll(tryUnlock(userId, UserReward.RewardType.FIRST_LEVEL_UP));
        if (characterLevel >= 3) unlocked.addAll(tryUnlock(userId, UserReward.RewardType.LEVEL_3_REACHED));

        if (hasPerfectDay) unlocked.addAll(tryUnlock(userId, UserReward.RewardType.PERFECT_DAY));

        return unlocked;
    }

    private boolean hasPerfectDayToday(Long userId) {
        LocalDate today = LocalDate.now(ALMATY_ZONE);
        long completedToday = taskRepository.countByUserIdAndTaskDateAndIsCompletedTrue(userId, today);
        return completedToday >= DAILY_TASK_COUNT;
    }

    // ========== HELPER METHODS ==========

    private List<UserReward> tryUnlock(Long userId, UserReward.RewardType type) {
        UserReward reward = rewardRepository.findByUserIdAndRewardType(userId, type)
                .orElseGet(() -> {
                    UserReward r = UserReward.builder()
                            .userId(userId)
                            .rewardType(type)
                            .isUnlocked(false)
                            .xpMultiplier(type.getXpMultiplier())
                            .build();
                    return rewardRepository.save(r);
                });

        if (!reward.getIsUnlocked()) {
            reward.unlock();
            reward.setXpMultiplier(type.getXpMultiplier());
            rewardRepository.save(reward);
            log.info("Reward unlocked: {} for user {}", type, userId);
            return List.of(reward);
        }
        return List.of();
    }

    private UserReward getOrCreateRewardReadOnly(Long userId, UserReward.RewardType rewardType) {
        return rewardRepository.findByUserIdAndRewardType(userId, rewardType)
                .orElseGet(() -> {
                    UserReward r = UserReward.builder()
                            .userId(userId)
                            .rewardType(rewardType)
                            .isUnlocked(false)
                            .xpMultiplier(rewardType.getXpMultiplier())
                            .build();
                    return r; // not saved — read-only context, just for display
                });
    }

    private RewardResponse buildRewardResponse(UserReward reward) {
        return RewardResponse.builder()
                .id(reward.getId())
                .rewardType(reward.getRewardType())
                .title(reward.getRewardType().getDisplayName())
                .description(reward.getRewardType().getDescription())
                .xpMultiplier(reward.getXpMultiplier())
                .isUnlocked(reward.getIsUnlocked())
                .unlockedAt(reward.getUnlockedAt())
                .requiredStreak(reward.getRewardType().getRequiredStreak())
                .build();
    }
}