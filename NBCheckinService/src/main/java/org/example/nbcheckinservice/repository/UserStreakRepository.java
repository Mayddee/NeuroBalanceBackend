package org.example.nbcheckinservice.repository;

import org.example.nbcheckinservice.entity.UserStreak;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserStreakRepository extends JpaRepository<UserStreak, Long> {
    Optional<UserStreak> findByUserId(Long userId);

    // Топ-10 по текущему стрейку
    List<UserStreak> findTop10ByOrderByCurrentStreakDesc();

    // Топ-10 по общему XP
    List<UserStreak> findTop10ByOrderByTotalXpEarnedDesc();

    // Получить ранг пользователя по стрейку
    @Query(value = "SELECT count(*) + 1 FROM user_streaks WHERE current_streak > (SELECT current_streak FROM user_streaks WHERE user_id = :userId)", nativeQuery = true)
    Long getUserRankByCurrentStreak(@Param("userId") Long userId);

    // Получить ранг пользователя по XP
    @Query(value = "SELECT count(*) + 1 FROM user_streaks WHERE total_xp_earned > (SELECT total_xp_earned FROM user_streaks WHERE user_id = :userId)", nativeQuery = true)
    Long getUserRankByTotalXp(@Param("userId") Long userId);
}