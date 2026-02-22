package org.example.nbcheckinservice.repository;


import org.example.nbcheckinservice.entity.DailyCheckIn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;


@Repository
public interface DailyCheckInRepository extends JpaRepository<DailyCheckIn, Long> {


    Optional<DailyCheckIn> findByUserIdAndCheckInDate(Long userId, LocalDate date);


    boolean existsByUserIdAndCheckInDate(Long userId, LocalDate date);


    List<DailyCheckIn> findByUserIdAndCheckInDateBetweenOrderByCheckInDateDesc(
            Long userId,
            LocalDate startDate,
            LocalDate endDate
    );


    List<DailyCheckIn> findByUserIdOrderByCheckInDateDesc(Long userId);


    List<DailyCheckIn> findTop30ByUserIdOrderByCheckInDateDesc(Long userId);


    long countByUserId(Long userId);


    List<DailyCheckIn> findByUserIdAndPlayedCognitiveGameTodayTrue(Long userId);


    @Query("SELECT AVG(CASE WHEN d.morningMood IS NOT NULL AND d.eveningMood IS NOT NULL " +
            "THEN (d.morningMood + d.eveningMood) / 2.0 " +
            "WHEN d.morningMood IS NOT NULL THEN d.morningMood " +
            "WHEN d.eveningMood IS NOT NULL THEN d.eveningMood " +
            "ELSE NULL END) " +
            "FROM DailyCheckIn d " +
            "WHERE d.userId = :userId " +
            "AND d.checkInDate BETWEEN :startDate AND :endDate")
    Double getAverageMood(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );


    @Query("SELECT AVG(d.sleepQuality) FROM DailyCheckIn d " +
            "WHERE d.userId = :userId " +
            "AND d.checkInDate BETWEEN :startDate AND :endDate " +
            "AND d.sleepQuality IS NOT NULL")
    Double getAverageSleepQuality(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );


    @Query("SELECT AVG(d.sleepHours) FROM DailyCheckIn d " +
            "WHERE d.userId = :userId " +
            "AND d.checkInDate BETWEEN :startDate AND :endDate " +
            "AND d.sleepHours IS NOT NULL")
    Double getAverageSleepHours(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );


    @Query("SELECT AVG(d.stressLevel) FROM DailyCheckIn d " +
            "WHERE d.userId = :userId " +
            "AND d.checkInDate BETWEEN :startDate AND :endDate " +
            "AND d.stressLevel IS NOT NULL")
    Double getAverageStressLevel(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );


    @Query("SELECT AVG(d.energyLevel) FROM DailyCheckIn d " +
            "WHERE d.userId = :userId " +
            "AND d.checkInDate BETWEEN :startDate AND :endDate " +
            "AND d.energyLevel IS NOT NULL")
    Double getAverageEnergyLevel(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );


    @Query("SELECT COUNT(d) FROM DailyCheckIn d " +
            "WHERE d.userId = :userId " +
            "AND d.checkInDate BETWEEN :startDate AND :endDate " +
            "AND d.didExercise = true")
    long countExerciseDays(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Sum total physical activity minutes in date range
     */
    @Query("SELECT COALESCE(SUM(d.physicalActivityMinutes), 0) FROM DailyCheckIn d " +
            "WHERE d.userId = :userId " +
            "AND d.checkInDate BETWEEN :startDate AND :endDate")
    Integer getTotalPhysicalActivityMinutes(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Count days user played cognitive games
     */
    @Query("SELECT COUNT(d) FROM DailyCheckIn d " +
            "WHERE d.userId = :userId " +
            "AND d.checkInDate BETWEEN :startDate AND :endDate " +
            "AND d.playedCognitiveGameToday = true")
    long countCognitiveGameDays(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Delete check-ins older than a certain date
     */
    void deleteByCheckInDateBefore(LocalDate date);
}