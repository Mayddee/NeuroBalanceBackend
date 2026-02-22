package org.example.nbcheckinservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response DTO for sleep log data
 * Path: mental-health-service/src/main/java/com/neuralbalance/mentalhealth/dto/sleep/SleepLogResponse.java
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


    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime bedtime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime wakeTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime fellAsleepTime;


    private BigDecimal totalHours;
    private BigDecimal actualSleepHours;
    private Integer timeToFallAsleepMinutes;


    private Integer qualityScore;
    private BigDecimal sleepEfficiency;
    private Boolean feltRested;
    private Double sleepScore; // Calculated score 0-100


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


    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
