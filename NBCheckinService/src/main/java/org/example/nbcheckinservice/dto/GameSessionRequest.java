package org.example.nbcheckinservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.nbcheckinservice.entity.GameSession;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameSessionRequest {

    @NotNull(message = "Game type is required")
    private GameSession.GameType gameType;

    @Min(value = 0, message = "Duration cannot be negative")
    private Integer durationSeconds;

    @NotNull(message = "Completion status is required")
    private Boolean isCompleted;

    @NotNull(message = "Win status is required")
    private Boolean isWon;

    /** Optional: record for a past date (Asia/Almaty). Defaults to today if not provided. */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate gameDate;
}