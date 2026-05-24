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

    /**
     * Number of attempts/interactions during the session.
     * For NUMBER_SEQUENCE_GAME: number of tries (fewer = better score).
     * For CHARACTER_CARE: number of care actions performed (more = better score).
     * Optional — games that don't use it can omit this field.
     */
    @Min(value = 1, message = "Attempts count must be at least 1")
    private Integer attemptsCount;

    /**
     * Difficulty level of the session: EASY, MEDIUM, HARD.
     * Applies an XP multiplier: EASY=1.0×, MEDIUM=1.5×, HARD=2.5×.
     * Optional — defaults to EASY.
     */
    private String difficultyLevel;

    /** Optional: record for a past date (Asia/Almaty). Defaults to today if not provided. */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate gameDate;
}