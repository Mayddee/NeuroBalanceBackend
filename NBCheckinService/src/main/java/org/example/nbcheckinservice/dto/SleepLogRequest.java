package org.example.nbcheckinservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Request DTO for creating/updating sleep log
 * ✅ FIXED: Use LocalTime (HH:mm:ss) for time fields to match Entity
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SleepLogRequest {

    // ========== DATE ==========

    @NotNull(message = "Sleep date is required")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate sleepDate;

    // ========== TIMING (LocalTime - HH:mm:ss format) ==========

    @NotNull(message = "Bedtime is required")
    @JsonFormat(pattern = "HH:mm:ss[.SSS]")
    private LocalTime bedtime;

    @NotNull(message = "Wake time is required")
    @JsonFormat(pattern = "HH:mm:ss[.SSS]")
    private LocalTime wakeTime;

    @JsonFormat(pattern = "HH:mm:ss[.SSS]")
    private LocalTime fellAsleepTime;

    // ========== SLEEP DURATION ==========

    @Min(value = 0, message = "Total hours must be non-negative")
    @Max(value = 24, message = "Total hours cannot exceed 24")
    private Double totalHours;

    @Min(value = 0, message = "Actual sleep hours must be non-negative")
    @Max(value = 24, message = "Actual sleep hours cannot exceed 24")
    private Double actualSleepHours;

    @Min(value = 0, message = "Time to fall asleep must be non-negative")
    private Integer timeToFallAsleepMinutes;

    // ========== QUALITY ==========

    @Min(value = 1, message = "Quality score must be between 1 and 10")
    @Max(value = 10, message = "Quality score must be between 1 and 10")
    private Integer qualityScore;

    private Boolean feltRested;

    // ========== INTERRUPTIONS ==========

    @Min(value = 0, message = "Interruptions count must be non-negative")
    private Integer interruptionsCount;

    @Min(value = 0, message = "Awake duration must be non-negative")
    private Integer awakeDurationMinutes;

    @Min(value = 0, message = "Bathroom trips must be non-negative")
    private Integer bathroomTrips;

    // ========== SLEEP STAGES ==========

    @Min(value = 0, message = "Deep sleep minutes must be non-negative")
    private Integer deepSleepMinutes;

    @Min(value = 0, message = "Light sleep minutes must be non-negative")
    private Integer lightSleepMinutes;

    @Min(value = 0, message = "REM sleep minutes must be non-negative")
    private Integer remSleepMinutes;

    @Min(value = 0, message = "Awake minutes must be non-negative")
    private Integer awakeMinutes;

    // ========== DREAMS ==========

    private Boolean hadDreams;

    @Size(max = 50, message = "Dream recall must not exceed 50 characters")
    private String dreamRecall;

    @Size(max = 2000, message = "Dream notes must not exceed 2000 characters")
    private String dreamNotes;

    private Boolean nightmares;

    // ========== ENVIRONMENT ==========

    @Size(max = 50, message = "Room temperature must not exceed 50 characters")
    private String roomTemperature;

    @Size(max = 50, message = "Noise level must not exceed 50 characters")
    private String noiseLevel;

    @Size(max = 50, message = "Light level must not exceed 50 characters")
    private String lightLevel;

    @Size(max = 50, message = "Bed comfort must not exceed 50 characters")
    private String bedComfort;

    // ========== PRE-SLEEP FACTORS ==========

    private Boolean caffeineBeforeBed;

    @Min(value = 0, message = "Screen time must be non-negative")
    private Integer screenTimeBeforeBedMinutes;

    private Boolean exerciseBeforeBed;

    private Boolean alcohol;

    private Boolean heavyMeal;

    // ========== MORNING METRICS ==========

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