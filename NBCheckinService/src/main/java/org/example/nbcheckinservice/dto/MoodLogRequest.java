package org.example.nbcheckinservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Request DTO for creating/updating mood log
 * Path: mental-health-service/src/main/java/com/neuralbalance/mentalhealth/dto/mood/MoodLogRequest.java
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MoodLogRequest {

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime logTimestamp; // Optional, defaults to now

    @NotNull(message = "Mood value is required")
    @Min(value = 1, message = "Mood value must be between 1 and 5")
    @Max(value = 5, message = "Mood value must be between 1 and 5")
    private Integer moodValue;

    @Size(max = 50, message = "Mood label must not exceed 50 characters")
    private String moodLabel; // happy, sad, anxious, stressed, excited, angry, calm, tired

    @Min(value = 1, message = "Intensity must be between 1 and 10")
    @Max(value = 10, message = "Intensity must be between 1 and 10")
    private Integer intensity;

    @Size(max = 1000, message = "Context note must not exceed 1000 characters")
    private String contextNote;

    @Size(max = 100, message = "Location must not exceed 100 characters")
    private String location; // home, work, school, gym, outside, transport

    @Size(max = 100, message = "Activity must not exceed 100 characters")
    private String activity; // working, studying, exercising, socializing, relaxing

    private List<String> triggers; // work_stress, relationship, health, achievement, social

    private List<String> physicalSensations; // headache, tension, energy, fatigue, restlessness
}