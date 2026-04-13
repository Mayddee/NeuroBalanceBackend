package org.example.nbcheckinservice.repository;

import org.example.nbcheckinservice.entity.NewGameSession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface NewGameSessionRepository extends JpaRepository<NewGameSession, Long> {
    List<NewGameSession> findByUserIdAndPlayedAtBetweenOrderByPlayedAtDesc(Long userId, LocalDateTime start, LocalDateTime end);
    List<NewGameSession> findByUserIdAndGameType(Long userId, NewGameSession.GameType gameType);

    boolean existsByUserIdAndPlayedAtBetween(Long userId, LocalDateTime start, LocalDateTime end);
}