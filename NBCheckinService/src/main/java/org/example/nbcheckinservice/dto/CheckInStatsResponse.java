package org.example.nbcheckinservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Response DTO for check-in statistics and analytics
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckInStatsResponse {

    private Long userId;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;


    private Double avgMorningMood;
    private Double avgEveningMood;
    private Double avgSleepQuality;
    private Double avgSleepHours;
    private Double avgEnergyLevel;
    private Double avgStressLevel;
    private Double avgWellnessScore;


    private Integer totalCheckIns;
    private Integer totalPhysicalActivityMinutes;
    private Integer totalCognitiveGamesPlayed;


    private Double exercisePercentage;      // % of days user exercised
    private Double healthyEatingPercentage; // % of days user ate healthy
    private Double socialInteractionPercentage;
    private Double cognitiveGamePercentage; // % of days user played games


    private String moodTrend;       // improving, stable, declining
    private String sleepTrend;      // improving, stable, declining
    private String stressTrend;     // improving, stable, declining
    private String energyTrend;     // improving, stable, declining


    private List<DailyData> dailyData;

    @Data
    @Builder
    public static class DailyData {
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate date;

        private Integer morningMood;
        private Integer eveningMood;
        private Integer sleepQuality;
        private Integer energyLevel;
        private Integer stressLevel;
        private Double wellnessScore;
        private Boolean hasCheckIn;
    }


    private List<String> insights;


    private Map<String, Integer> moodDistribution; // emoji -> count


    private LocalDate bestDay;      // Day with highest wellness score
    private LocalDate worstDay;     // Day with lowest wellness score
    private Double bestWellnessScore;
    private Double worstWellnessScore;
}