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

    @Builder.Default
    @Column(name = "total_games_played", nullable = false)
    private Integer totalGamesPlayed = 0;

    @Builder.Default
    @Column(name = "total_xp_earned", nullable = false)
    private Integer totalXpEarned = 0;

    @Builder.Default
    @Column(name = "total_wins", nullable = false)
    private Integer totalWins = 0;

    @Builder.Default
    @Column(name = "total_losses", nullable = false)
    private Integer totalLosses = 0;

    @Column(name = "number_sequence_best_time")
    private Integer numberSequenceBestTime;

    @Column(name = "memory_pairs_best_time")
    private Integer memoryPairsBestTime;

    @Builder.Default
    @Column(name = "current_streak", nullable = false)
    private Integer currentStreak = 0;

    @Builder.Default
    @Column(name = "best_streak", nullable = false)
    private Integer bestStreak = 0;

    @Column(name = "last_played_date")
    private LocalDate lastPlayedDate;

    // --- Вспомогательные методы (бизнес-логика) ---

    public void incrementGamesPlayed() {
        this.totalGamesPlayed++;
    }

    public void addXp(Integer xp) {
        if (xp != null) {
            this.totalXpEarned += xp;
        }
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
        if (timeTaken == null) return;
        
        if (gameType == BrainGameResult.GameType.NUMBER_SEQUENCE) {
            if (this.numberSequenceBestTime == null || timeTaken < this.numberSequenceBestTime) {
                this.numberSequenceBestTime = timeTaken;
            }
        } else if (gameType == BrainGameResult.GameType.MEMORY_PAIRS) {
            if (this.memoryPairsBestTime == null || timeTaken < this.memoryPairsBestTime) {
                this.memoryPairsBestTime = timeTaken;
            }
        }
    }
}