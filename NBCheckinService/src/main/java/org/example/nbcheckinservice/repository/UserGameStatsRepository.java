package org.example.nbcheckinservice.repository;


import org.example.nbcheckinservice.entity.UserGameStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserGameStatsRepository extends JpaRepository<UserGameStats, Long> {

    Optional<UserGameStats> findByUserId(Long userId);
}