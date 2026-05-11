package org.example.nbcheckinservice.kafka;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameCompletedEvent {
    private Long userId;
    private String gameType;         // NUMBER_SEQUENCE | MEMORY_PAIRS | DONUT_GAME | ...
    private String difficultyLevel;  // EASY | MEDIUM | HARD
    private Boolean isWin;
    private Integer xpEarned;
    private String playedAt;         // ISO datetime string
}
