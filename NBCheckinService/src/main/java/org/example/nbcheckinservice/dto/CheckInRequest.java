package org.example.nbcheckinservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Request DTO for creating/updating daily check-in
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckInRequest {

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate checkInDate; // Optional, defaults to today


    @Min(value = 1, message = "Morning mood must be between 1 and 5")
    @Max(value = 5, message = "Morning mood must be between 1 and 5")
    private Integer morningMood;

    @Min(value = 1, message = "Evening mood must be between 1 and 5")
    @Max(value = 5, message = "Evening mood must be between 1 and 5")
    private Integer eveningMood;


    @NotNull(message = "Sleep quality is required")
    @Min(value = 1, message = "Sleep quality must be between 1 and 10")
    @Max(value = 10, message = "Sleep quality must be between 1 and 10")
    private Integer sleepQuality;

    @DecimalMin(value = "0.0", message = "Sleep hours cannot be negative")
    @DecimalMax(value = "24.0", message = "Sleep hours cannot exceed 24")
    private BigDecimal sleepHours;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime sleepBedtime;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime sleepWaketime;


    @NotNull(message = "Energy level is required")
    @Min(value = 1, message = "Energy level must be between 1 and 10")
    @Max(value = 10, message = "Energy level must be between 1 and 10")
    private Integer energyLevel;

    @NotNull(message = "Stress level is required")
    @Min(value = 1, message = "Stress level must be between 1 and 10")
    @Max(value = 10, message = "Stress level must be between 1 and 10")
    private Integer stressLevel;


    @Min(value = 0, message = "Activity minutes cannot be negative")
    private Integer physicalActivityMinutes;

    @Pattern(regexp = "walk|gym|yoga|sports|none",
            message = "Activity type must be one of: walk, gym, yoga, sports, none")
    private String physicalActivityType;


    private Boolean didExercise;
    private Boolean ateHealthy;
    private Boolean hadSocialInteraction;


    private Boolean playedCognitiveGameToday;

    @Min(value = 0, message = "Cognitive game count cannot be negative")
    private Integer cognitiveGameCount;
}