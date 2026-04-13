package org.example.nbcheckinservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "user_game_stats")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserGameStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "total_games_played", nullable = false)
    private Integer totalGamesPlayed = 0;

    @Column(name = "total_xp_earned", nullable = false)
    private Integer totalXpEarned = 0;

    @Column(name = "total_wins", nullable = false)
    private Integer totalWins = 0;

    @Column(name = "total_losses", nullable = false)
    private Integer totalLosses = 0;

    @Column(name = "number_sequence_best_time")
    private Integer numberSequenceBestTime;

    @Column(name = "memory_pairs_best_time")
    private Integer memoryPairsBestTime;

    @Column(name = "current_streak", nullable = false)
    private Integer currentStreak = 0;

    @Column(name = "best_streak", nullable = false)
    private Integer bestStreak = 0;

    @Column(name = "last_played_date")
    private LocalDate lastPlayedDate;

    // Вспомогательные методы
    public void incrementGamesPlayed() {
        this.totalGamesPlayed++;
    }

    public void addXp(Integer xp) {
        this.totalXpEarned += xp;
    }

    public void recordWin() {
        this.totalWins++;
        this.currentStreak++;
        if (this.currentStreak > this.bestStreak) {
            this.bestStreak = this.currentStreak;
        }
    }

    public void recordLoss() {
        this.totalLosses++;
        this.currentStreak = 0;
    }

    public void updateBestTime(BrainGameResult.GameType gameType, Integer timeTaken) {
        if (gameType == BrainGameResult.GameType.NUMBER_SEQUENCE) {
            if (numberSequenceBestTime == null || timeTaken < numberSequenceBestTime) {
                numberSequenceBestTime = timeTaken;
            }
        } else if (gameType == BrainGameResult.GameType.MEMORY_PAIRS) {
            if (memoryPairsBestTime == null || timeTaken < memoryPairsBestTime) {
                memoryPairsBestTime = timeTaken;
            }
        }
    }
}
