package org.example.nbcheckinservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Response DTO for sleep log
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SleepLogResponse {

    private Long id;
    private Long userId;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate sleepDate;

    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime bedtime;

    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime wakeTime;

    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime fellAsleepTime;

    private Double totalHours;
    private Double actualSleepHours;
    private Integer timeToFallAsleepMinutes;
    private Double sleepEfficiency;

    private Integer qualityScore;
    private Boolean feltRested;

    private Integer interruptionsCount;
    private Integer awakeDurationMinutes;
    private Integer bathroomTrips;

    private Integer deepSleepMinutes;
    private Integer lightSleepMinutes;
    private Integer remSleepMinutes;
    private Integer awakeMinutes;

    private Boolean hadDreams;
    private String dreamRecall;
    private String dreamNotes;
    private Boolean nightmares;

    private String roomTemperature;
    private String noiseLevel;
    private String lightLevel;
    private String bedComfort;

    private Boolean caffeineBeforeBed;
    private Integer screenTimeBeforeBedMinutes;
    private Boolean exerciseBeforeBed;
    private Boolean alcohol;
    private Boolean heavyMeal;

    private Integer morningMood;
    private Integer morningEnergy;

    private String notes;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
}