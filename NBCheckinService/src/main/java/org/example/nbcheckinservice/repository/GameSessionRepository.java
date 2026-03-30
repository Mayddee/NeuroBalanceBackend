package org.example.nbcheckinservice.repository;

import org.example.nbcheckinservice.entity.GameSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface GameSessionRepository extends JpaRepository<GameSession, Long> {

    List<GameSession> findByUserIdAndPlayedAtBetweenOrderByPlayedAtDesc(
            Long userId,
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    List<GameSession> findTop10ByUserIdOrderByPlayedAtDesc(Long userId);

    boolean existsByUserIdAndPlayedAtBetween(
            Long userId,
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    long countByUserIdAndIsWonTrue(Long userId);
}