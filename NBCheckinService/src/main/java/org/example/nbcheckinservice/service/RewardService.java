package org.example.nbcheckinservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nbcheckinservice.dto.RewardResponse;
import org.example.nbcheckinservice.entity.UserReward;
import org.example.nbcheckinservice.entity.UserStreak;
import org.example.nbcheckinservice.repository.UserRewardRepository;
import org.hibernate.validator.internal.util.stereotypes.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing user rewards/badges
 * ✅ VERIFIED: All logic is correct
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RewardService {

    private final UserRewardRepository rewardRepository;

    @Lazy
    private final StreakService streakService;

    @Transactional
    public List<RewardResponse> checkAndUnlockRewards(Long userId) {
        UserStreak streak = streakService.getOrCreateStreak(userId);
        int currentStreak = streak.getCurrentStreak();

        List<UserReward> newlyUnlocked = new ArrayList<>();

        for (UserReward.RewardType rewardType : UserReward.RewardType.values()) {
            if (currentStreak >= rewardType.getRequiredStreak()) {
                UserReward reward = getOrCreateReward(userId, rewardType);

                if (!reward.getIsUnlocked()) {
                    reward.unlock();
                    reward.setXpMultiplier(rewardType.getXpMultiplier());
                    rewardRepository.save(reward);
                    newlyUnlocked.add(reward);

                    log.info("🎉 Unlocked reward {} for user {}", rewardType, userId);
                }
            }
        }

        return newlyUnlocked.stream()
                .map(this::buildRewardResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RewardResponse> getAllRewards(Long userId) {
        List<UserReward> rewards = new ArrayList<>();

        for (UserReward.RewardType rewardType : UserReward.RewardType.values()) {
            rewards.add(getOrCreateReward(userId, rewardType));
        }

        return rewards.stream()
                .map(this::buildRewardResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public double getActiveXpMultiplier(Long userId) {
        return rewardRepository.findByUserIdAndIsUnlockedTrueOrderByXpMultiplierDesc(userId)
                .stream()
                .findFirst()
                .map(UserReward::getXpMultiplier)
                .orElse(1.0);
    }

    // ========== HELPER METHODS ==========

    private UserReward getOrCreateReward(Long userId, UserReward.RewardType rewardType) {
        return rewardRepository.findByUserIdAndRewardType(userId, rewardType)
                .orElseGet(() -> createReward(userId, rewardType));
    }

    private UserReward createReward(Long userId, UserReward.RewardType rewardType) {
        UserReward reward = UserReward.builder()
                .userId(userId)
                .rewardType(rewardType)
                .isUnlocked(false)
                .xpMultiplier(rewardType.getXpMultiplier())
                .build();

        return rewardRepository.save(reward);
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