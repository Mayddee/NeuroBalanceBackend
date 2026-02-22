package org.example.nbauthservice.repository;

import org.example.nbauthservice.entity.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {
    Optional<VerificationToken> findByToken(String token);

    Optional<VerificationToken> findByUserId(Long userId);

    @Modifying
    @Query("DELETE FROM VerificationToken v WHERE v.expiryDate < :now")
    int deleteExpiredTokens(@Param("now") LocalDateTime now);
}
