package org.example.nbcheckinservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CharacterResponse {

    private Long userId;
    private String characterType;
    private String characterName;
    private String characterEmoji;

    private Integer currentLevel;
    private Integer totalXp;
    private Integer xpForNextLevel;
    private Double levelProgress; // Percentage

    private Integer happinessLevel;
    private Integer energyLevel;

    private String description;
    private Boolean justLeveledUp;
    private Boolean isMaxLevel;

    private LocalDateTime lastInteractionAt;
}