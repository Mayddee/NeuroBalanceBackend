package org.example.nbcheckinservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.nbcheckinservice.entity.NewGameSession;

import java.time.LocalDate;

/**
 * Request DTO for NewGameSessionService.
 * Accepts DONUT_GAME or NUMBER_SEQUENCE_GAME only.
 * Kept separate from GameSessionRequest to avoid enum type mismatch.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewGameSessionRequest {

    @NotNull(message = "Game type is required (DONUT_GAME or NUMBER_SEQUENCE_GAME)")
    private NewGameSession.GameType gameType;

    @Min(value = 0, message = "Duration cannot be negative")
    private Integer durationSeconds;

    @NotNull(message = "Completion status is required")
    private Boolean isCompleted;

    @NotNull(message = "Win status is required")
    private Boolean isWon;

    /**
     * NUMBER_SEQUENCE_GAME: number of tries — fewer = higher bonus (1 try: +30 XP, 2: +15, 3: +5).
     * DONUT_GAME: not used.
     */
    @Min(value = 1, message = "Attempts count must be at least 1")
    private Integer attemptsCount;

    /**
     * Difficulty level: EASY, MEDIUM, HARD.
     * EASY=1.0×, MEDIUM=1.5×, HARD=2.5× XP multiplier (applied after all bonuses).
     * Optional — defaults to EASY.
     */
    private String difficultyLevel;

    /**
     * Optional. Record for a past date (Asia/Almaty timezone).
     * Defaults to today if not provided.
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate gameDate;
}