package org.example.nbcheckinservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GameResultResponse {

    private Long resultId;
    private Integer xpEarned;
    private Integer totalXp;
    private Boolean isWin;
    private String message;
    private Integer currentStreak;
    private Boolean isNewBestTime;
}