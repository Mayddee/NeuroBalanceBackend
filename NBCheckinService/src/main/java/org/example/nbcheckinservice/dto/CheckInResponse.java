package org.example.nbcheckinservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

/**
 * Response DTO for daily check-in data
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckInResponse {

    private Long id;
    private Long userId;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate checkInDate;


    private Integer morningMood;
    private Integer eveningMood;
    private String morningMoodEmoji;
    private String eveningMoodEmoji;


    private Integer sleepQuality;
    private BigDecimal sleepHours;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime sleepBedtime;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime sleepWaketime;


    private Integer energyLevel;
    private Integer stressLevel;


    private Integer physicalActivityMinutes;
    private String physicalActivityType;


    private Boolean didExercise;
    private Boolean ateHealthy;
    private Boolean hadSocialInteraction;


    private Boolean playedCognitiveGameToday;
    private Integer cognitiveGameCount;


    private Double wellnessScore; // 0-100
    private Boolean isComplete;


    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;


    private StreakInfo streakInfo;

    @Data
    @Builder
    public static class StreakInfo {
        private Integer currentStreak;
        private Integer longestStreak;
        private Integer xpEarned;
        private Integer bonusXp;
        private Boolean isMilestone;
        private String message;
        private String nextMilestone;
    }
}
