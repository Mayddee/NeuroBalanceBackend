package org.example.nbcheckinservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Request DTO for creating/updating mood log
 * ✅ FIXED: Support milliseconds in timestamp format
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MoodLogRequest {

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss[.SSS]")
    private LocalDateTime logTimestamp;

    @NotNull(message = "Mood value is required")
    @Min(value = 1, message = "Mood value must be between 1 and 5")
    @Max(value = 5, message = "Mood value must be between 1 and 5")
    private Integer moodValue;

    @Size(max = 50, message = "Mood label must not exceed 50 characters")
    private String moodLabel;

    @Min(value = 1, message = "Intensity must be between 1 and 10")
    @Max(value = 10, message = "Intensity must be between 1 and 10")
    private Integer intensity;

    @Size(max = 1000, message = "Context note must not exceed 1000 characters")
    private String contextNote;

    @Size(max = 100, message = "Location must not exceed 100 characters")
    private String location;

    @Size(max = 100, message = "Activity must not exceed 100 characters")
    private String activity;

    private List<String> triggers;

    private List<String> physicalSensations;
}