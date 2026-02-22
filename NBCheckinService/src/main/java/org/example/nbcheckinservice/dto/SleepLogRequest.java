package org.example.nbcheckinservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Request DTO for creating/updating sleep log
 * Path: mental-health-service/src/main/java/com/neuralbalance/mentalhealth/dto/sleep/SleepLogRequest.java
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SleepLogRequest {

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate sleepDate; // Optional, defaults to today

    // ========== TIMING ==========

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime bedtime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime wakeTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime fellAsleepTime;

    // ========== DURATION ==========

    @DecimalMin(value = "0.0", message = "Total hours cannot be negative")
    @DecimalMax(value = "24.0", message = "Total hours cannot exceed 24")
    private BigDecimal totalHours;

    @DecimalMin(value = "0.0", message = "Actual sleep hours cannot be negative")
    private BigDecimal actualSleepHours;

    @Min(value = 0, message = "Time to fall asleep cannot be negative")
    private Integer timeToFallAsleepMinutes;

    // ========== QUALITY ==========

    @NotNull(message = "Quality score is required")
    @Min(value = 1, message = "Quality score must be between 1 and 10")
    @Max(value = 10, message = "Quality score must be between 1 and 10")
    private Integer qualityScore;

    private Boolean feltRested;

    // ========== INTERRUPTIONS ==========

    @Min(value = 0, message = "Interruptions count cannot be negative")
    private Integer interruptionsCount;

    @Min(value = 0, message = "Awake duration cannot be negative")
    private Integer awakeDurationMinutes;

    @Min(value = 0, message = "Bathroom trips cannot be negative")
    private Integer bathroomTrips;

    // ========== SLEEP STAGES ==========

    @Min(value = 0, message = "Deep sleep minutes cannot be negative")
    private Integer deepSleepMinutes;

    @Min(value = 0, message = "Light sleep minutes cannot be negative")
    private Integer lightSleepMinutes;

    @Min(value = 0, message = "REM sleep minutes cannot be negative")
    private Integer remSleepMinutes;

    @Min(value = 0, message = "Awake minutes cannot be negative")
    private Integer awakeMinutes;

    // ========== DREAMS ==========

    private Boolean hadDreams;

    @Pattern(regexp = "none|vague|clear|vivid",
            message = "Dream recall must be: none, vague, clear, or vivid")
    private String dreamRecall;

    @Size(max = 1000, message = "Dream notes must not exceed 1000 characters")
    private String dreamNotes;

    private Boolean nightmares;

    // ========== ENVIRONMENT ==========

    @Pattern(regexp = "cold|comfortable|warm|hot",
            message = "Room temperature must be: cold, comfortable, warm, or hot")
    private String roomTemperature;

    @Pattern(regexp = "silent|quiet|moderate|noisy",
            message = "Noise level must be: silent, quiet, moderate, or noisy")
    private String noiseLevel;

    @Pattern(regexp = "dark|dim|bright",
            message = "Light level must be: dark, dim, or bright")
    private String lightLevel;

    @Pattern(regexp = "uncomfortable|okay|comfortable|very_comfortable",
            message = "Bed comfort must be: uncomfortable, okay, comfortable, or very_comfortable")
    private String bedComfort;

    // ========== PRE-SLEEP FACTORS ==========

    private Boolean caffeineBeforeBed;

    @Min(value = 0, message = "Screen time cannot be negative")
    private Integer screenTimeBeforeBedMinutes;

    private Boolean exerciseBeforeBed;
    private Boolean alcohol;
    private Boolean heavyMeal;

    // ========== MORNING FEELING ==========

    @Min(value = 1, message = "Morning mood must be between 1 and 5")
    @Max(value = 5, message = "Morning mood must be between 1 and 5")
    private Integer morningMood;

    @Min(value = 1, message = "Morning energy must be between 1 and 10")
    @Max(value = 10, message = "Morning energy must be between 1 and 10")
    private Integer morningEnergy;

    // ========== NOTES ==========

    @Size(max = 2000, message = "Notes must not exceed 2000 characters")
    private String notes;
}
