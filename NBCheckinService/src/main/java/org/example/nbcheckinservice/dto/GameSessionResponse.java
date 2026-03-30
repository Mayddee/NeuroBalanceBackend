package org.example.nbcheckinservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.nbcheckinservice.entity.GameSession;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameSessionResponse {

    private Long id;
    private Long userId;

    private GameSession.GameType gameType;
    private String gameTypeName;

    private Integer durationSeconds;

    private Boolean isCompleted;
    private Boolean isWon;

    private Integer xpEarned;
    private Integer bonusXp;

    private LocalDateTime playedAt;
}