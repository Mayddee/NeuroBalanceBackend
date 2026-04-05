package org.example.nbauthservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * User Onboarding Data
 * Хранит метрики и ML данные пользователя
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "user_onboarding")
public class UserOnboarding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    @Column(name = "sex", length = 10)
    private String sex; // "MALE", "FEMALE", "OTHER"

    @Column(name = "height_cm")
    private Integer heightCm;

    @Column(name = "weight_kg")
    private Integer weightKg;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "character_id")
    private Integer characterId;

    @Column(name = "data_consent", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean dataConsent = false; // Согласие на сбор данных

    @Column(name = "is_completed", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean isCompleted = false;
}