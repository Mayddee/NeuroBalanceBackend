package org.example.nbcheckinservice.dto;


import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.example.nbcheckinservice.entity.BrainGameResult;


@Data
public class BrainGameSubmitRequest {

    @NotNull
    private BrainGameResult.GameType gameType;

    @NotNull
    @Min(0)
    private Integer score;

    @NotNull
    @Min(1)
    private Integer timeTakenSeconds;

    @NotNull
    private Boolean isWin;

    private String difficultyLevel = "MEDIUM";

    private Integer mistakesCount = 0;
}