package org.example.nbcheckinservice.repository;

import org.example.nbcheckinservice.entity.UserReward;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRewardRepository extends JpaRepository<UserReward, Long> {

    List<UserReward> findByUserId(Long userId);

    Optional<UserReward> findByUserIdAndRewardType(Long userId, UserReward.RewardType rewardType);

    List<UserReward> findByUserIdAndIsUnlockedTrueOrderByXpMultiplierDesc(Long userId);

    long countByUserIdAndIsUnlockedTrue(Long userId);
}