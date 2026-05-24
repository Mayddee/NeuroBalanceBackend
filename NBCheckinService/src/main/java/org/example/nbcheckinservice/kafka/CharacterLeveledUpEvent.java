package org.example.nbcheckinservice.kafka;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CharacterLeveledUpEvent {
    private Long userId;
    private Integer oldLevel;
    private Integer newLevel;
    private String characterType;
    private String characterEmoji;
    private Integer totalXp;
}