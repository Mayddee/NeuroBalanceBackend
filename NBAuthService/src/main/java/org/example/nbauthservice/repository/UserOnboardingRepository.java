package org.example.nbauthservice.repository;

import org.example.nbauthservice.entity.UserOnboarding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserOnboardingRepository extends JpaRepository<UserOnboarding, Long> {

    /**
     * Найти онбординг по user ID
     */
    Optional<UserOnboarding> findByUserId(Long userId);

    /**
     * Проверить существует ли онбординг для пользователя
     */
    boolean existsByUserId(Long userId);

    /**
     * Удалить онбординг по user ID
     */
    void deleteByUserId(Long userId);
}