package org.example.nbcheckinservice.repository;

import org.example.nbcheckinservice.entity.SleepLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;


@Repository
public interface SleepLogRepository extends JpaRepository<SleepLog, Long> {


    Optional<SleepLog> findByUserIdAndSleepDate(Long userId, LocalDate sleepDate);


    boolean existsByUserIdAndSleepDate(Long userId, LocalDate sleepDate);


    List<SleepLog> findByUserIdOrderBySleepDateDesc(Long userId);


    List<SleepLog> findByUserIdAndSleepDateBetweenOrderBySleepDateDesc(
            Long userId,
            LocalDate startDate,
            LocalDate endDate
    );


    List<SleepLog> findTop30ByUserIdOrderBySleepDateDesc(Long userId);


    long countByUserId(Long userId);


    @Query("SELECT AVG(s.qualityScore) FROM SleepLog s " +
            "WHERE s.userId = :userId " +
            "AND s.sleepDate BETWEEN :startDate AND :endDate " +
            "AND s.qualityScore IS NOT NULL")
    Double getAverageSleepQuality(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );


    @Query("SELECT AVG(s.actualSleepHours) FROM SleepLog s " +
            "WHERE s.userId = :userId " +
            "AND s.sleepDate BETWEEN :startDate AND :endDate " +
            "AND s.actualSleepHours IS NOT NULL")
    Double getAverageSleepHours(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );


    @Query("SELECT AVG(s.sleepEfficiency) FROM SleepLog s " +
            "WHERE s.userId = :userId " +
            "AND s.sleepDate BETWEEN :startDate AND :endDate " +
            "AND s.sleepEfficiency IS NOT NULL")
    Double getAverageSleepEfficiency(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );


    @Query("SELECT COUNT(s) FROM SleepLog s " +
            "WHERE s.userId = :userId " +
            "AND s.sleepDate BETWEEN :startDate AND :endDate " +
            "AND s.qualityScore >= 7")
    long countGoodSleepNights(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );


    @Query("SELECT COUNT(s) FROM SleepLog s " +
            "WHERE s.userId = :userId " +
            "AND s.sleepDate BETWEEN :startDate AND :endDate " +
            "AND s.hadDreams = true")
    long countNightsWithDreams(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );


    void deleteBySleepDateBefore(LocalDate date);
}