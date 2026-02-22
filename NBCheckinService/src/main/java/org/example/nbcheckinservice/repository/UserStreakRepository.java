package org.example.nbcheckinservice.repository;
import org.example.nbcheckinservice.entity.UserStreak;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface UserStreakRepository extends JpaRepository<UserStreak, Long> {


    Optional<UserStreak> findByUserId(Long userId);


    boolean existsByUserId(Long userId);


    List<UserStreak> findTop10ByOrderByCurrentStreakDesc();


    List<UserStreak> findTop10ByOrderByLongestStreakDesc();


    List<UserStreak> findTop10ByOrderByTotalXpEarnedDesc();


    @Query("SELECT COUNT(s) + 1 FROM UserStreak s " +
            "WHERE s.currentStreak > (SELECT s2.currentStreak FROM UserStreak s2 WHERE s2.userId = :userId)")
    Long getUserRankByCurrentStreak(Long userId);


    @Query("SELECT COUNT(s) + 1 FROM UserStreak s " +
            "WHERE s.totalXpEarned > (SELECT s2.totalXpEarned FROM UserStreak s2 WHERE s2.userId = :userId)")
    Long getUserRankByTotalXp(Long userId);
}
