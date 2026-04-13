package org.example.nbcheckinservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BrainGameStatsResponse {

    private Integer totalGamesPlayed;
    private Integer totalXpEarned;
    private Integer totalWins;
    private Integer totalLosses;
    private Double winRate;

    private Integer todayGamesPlayed;
    private Integer todayXpEarned;
    private Integer todayWins;

    private Integer currentStreak;
    private Integer bestStreak;

    private Integer numberSequenceBestTime;
    private Integer memoryPairsBestTime;

    private String lastPlayedDate;
}