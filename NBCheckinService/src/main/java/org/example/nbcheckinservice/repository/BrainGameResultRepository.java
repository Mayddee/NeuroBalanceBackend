package org.example.nbcheckinservice.repository;

import org.example.nbcheckinservice.entity.BrainGameResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BrainGameResultRepository extends JpaRepository<BrainGameResult, Long> {

    // Поиск всей истории игр пользователя (от новых к старым)
    List<BrainGameResult> findByUserIdOrderByPlayedAtDesc(Long userId);

    // Поиск истории по конкретному типу игры
    List<BrainGameResult> findByUserIdAndGameTypeOrderByPlayedAtDesc(
            Long userId,
            BrainGameResult.GameType gameType
    );

    // Поиск игр в заданном диапазоне (используется для получения списка игр за сегодня)
    @Query("SELECT r FROM BrainGameResult r WHERE r.userId = :userId " +
            "AND r.playedAt >= :startDate ORDER BY r.playedAt DESC")
    List<BrainGameResult> findUserGamesInDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate
    );

    // Подсчет количества игр за сегодня (с учетом переданных границ времени +5)
    @Query("SELECT COUNT(r) FROM BrainGameResult r WHERE r.userId = :userId " +
            "AND r.playedAt >= :start AND r.playedAt <= :end")
    Long countTodayGames(
            @Param("userId") Long userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    // Суммирование XP за сегодня (с учетом переданных границ времени +5)
    @Query("SELECT SUM(r.xpEarned) FROM BrainGameResult r WHERE r.userId = :userId " +
            "AND r.playedAt >= :start AND r.playedAt <= :end")
    Integer sumTodayXp(
            @Param("userId") Long userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}