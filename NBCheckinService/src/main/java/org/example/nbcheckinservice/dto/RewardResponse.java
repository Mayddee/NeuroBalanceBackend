package org.example.nbcheckinservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.nbcheckinservice.entity.UserReward;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RewardResponse {

    private Long id;
    private UserReward.RewardType rewardType;

    private String title;
    private String description;

    private Double xpMultiplier;
    private Boolean isUnlocked;
    private Integer requiredStreak;

    private LocalDateTime unlockedAt;
}