package org.example.nbcheckinservice.repository;

import org.example.nbcheckinservice.entity.DailyMLRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyMLRecommendationRepository extends JpaRepository<DailyMLRecommendation, Long> {

    Optional<DailyMLRecommendation> findByUserIdAndRecommendationDate(Long userId, LocalDate date);

    Optional<DailyMLRecommendation> findFirstByUserIdOrderByRecommendationDateDesc(Long userId);

    List<DailyMLRecommendation> findByUserIdOrderByRecommendationDateDesc(Long userId);
}
