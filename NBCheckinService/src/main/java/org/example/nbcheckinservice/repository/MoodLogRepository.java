package org.example.nbcheckinservice.repository;

import org.example.nbcheckinservice.entity.MoodLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;


@Repository
public interface MoodLogRepository extends JpaRepository<MoodLog, Long> {


    List<MoodLog> findByUserIdOrderByLogTimestampDesc(Long userId);


    List<MoodLog> findByUserIdAndLogTimestampBetweenOrderByLogTimestampDesc(
            Long userId,
            LocalDateTime startTime,
            LocalDateTime endTime
    );


    List<MoodLog> findTop20ByUserIdOrderByLogTimestampDesc(Long userId);


    long countByUserId(Long userId);


    List<MoodLog> findByUserIdAndMoodValue(Long userId, Integer moodValue);


    @Query("SELECT AVG(m.moodValue) FROM MoodLog m " +
            "WHERE m.userId = :userId " +
            "AND m.logTimestamp BETWEEN :startTime AND :endTime")
    Double getAverageMood(
            @Param("userId") Long userId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );


    @Query("SELECT COUNT(m) FROM MoodLog m " +
            "WHERE m.userId = :userId " +
            "AND m.moodValue = :moodValue " +
            "AND m.logTimestamp BETWEEN :startTime AND :endTime")
    long countByMoodValue(
            @Param("userId") Long userId,
            @Param("moodValue") Integer moodValue,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * Find mood logs with specific trigger
     */
    @Query("SELECT m FROM MoodLog m JOIN m.triggers t WHERE m.userId = :userId AND t = :trigger ORDER BY m.logTimestamp DESC")
    List<MoodLog> findByUserIdAndTrigger(@Param("userId") Long userId, @Param("trigger") String trigger);
    void deleteByLogTimestampBefore(LocalDateTime timestamp);
}
