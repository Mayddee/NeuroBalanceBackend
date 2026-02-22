package org.example.nbcheckinservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Response DTO for user streak information
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StreakResponse {

    private Long userId;
    private Integer currentStreak;
    private Integer longestStreak;
    private Integer totalCheckins;
    private Integer totalXpEarned;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate lastCheckinDate;

    private Boolean canCheckinToday;
    private String statusMessage;
    private String nextMilestone;
    private Boolean isMilestoneDay;
    private Integer milestoneBonus;
}