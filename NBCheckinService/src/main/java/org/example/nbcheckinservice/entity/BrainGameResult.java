package org.example.nbcheckinservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "brain_game_results")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrainGameResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "game_type", nullable = false)
    private GameType gameType;

    @Column(name = "score", nullable = false)
    private Integer score;

    @Column(name = "time_taken_seconds", nullable = false)
    private Integer timeTakenSeconds;

    @Column(name = "xp_earned", nullable = false)
    private Integer xpEarned;

    @Column(name = "is_win", nullable = false)
    private Boolean isWin;

    @Column(name = "difficulty_level")
    private String difficultyLevel; // EASY, MEDIUM, HARD

    @Column(name = "mistakes_count")
    private Integer mistakesCount;

    @Column(name = "played_at", nullable = false)
    private LocalDateTime playedAt;

    @PrePersist
    protected void onCreate() {
        playedAt = LocalDateTime.now();
    }

    public enum GameType {
        NUMBER_SEQUENCE,  // Число от 1 до 12
        MEMORY_PAIRS      // Карточки парами
    }
}