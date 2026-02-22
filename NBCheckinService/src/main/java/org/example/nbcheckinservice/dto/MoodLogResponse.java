package org.example.nbcheckinservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for mood log data
 * Path: mental-health-service/src/main/java/com/neuralbalance/mentalhealth/dto/mood/MoodLogResponse.java
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MoodLogResponse {

    private Long id;
    private Long userId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime logTimestamp;

    private Integer moodValue;
    private String moodEmoji;
    private String moodLabel;
    private Integer intensity;

    private String contextNote;
    private String location;
    private String activity;

    private List<String> triggers;
    private List<String> physicalSensations;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
