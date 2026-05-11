package org.example.nbcheckinservice.repository;

import org.example.nbcheckinservice.entity.HealthMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface HealthMetricsRepository extends JpaRepository<HealthMetrics, Long> {

    Optional<HealthMetrics> findByUserIdAndMetricDate(Long userId, LocalDate metricDate);

    Optional<HealthMetrics> findFirstByUserIdOrderByMetricDateDesc(Long userId);

    List<HealthMetrics> findByUserIdOrderByMetricDateDesc(Long userId);

    List<HealthMetrics> findByUserIdAndMetricDateBetweenOrderByMetricDateDesc(
            Long userId, LocalDate startDate, LocalDate endDate);

    boolean existsByUserIdAndMetricDate(Long userId, LocalDate metricDate);
}
