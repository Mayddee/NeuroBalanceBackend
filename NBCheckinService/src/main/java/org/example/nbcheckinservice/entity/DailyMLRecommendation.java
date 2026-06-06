package org.example.nbcheckinservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Persists daily ML recommendations per user so results survive server restarts
 * and are always based on real check-in / game / onboarding data
 * (not frontend-sent default params).
 *
 * One record per (user_id, recommendation_date) — upserted on each Kafka-triggered refresh.
 * GET /api/v1/ml/recommendations reads this table first before falling back to computation.
 */
@Entity
@Table(name = "daily_ml_recommendation", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "recommendation_date"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyMLRecommendation {

    private static final ZoneId ALMATY_ZONE = ZoneId.of("Asia/Almaty");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "recommendation_date", nullable = false)
    private LocalDate recommendationDate;

    /** Full MLRecommendationResponse serialized as JSON */
    @Column(name = "recommendations_json", columnDefinition = "TEXT", nullable = false)
    private String recommendationsJson;

    @Column(name = "cognitive_score")
    private Double cognitiveScore;

    /**
     * What triggered this computation:
     * checkin.created / sleep.logged / game.completed / on-demand / manual-refresh / db-stale
     */
    @Column(name = "trigger_source", length = 50)
    private String triggerSource;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now(ALMATY_ZONE);
        updatedAt = LocalDateTime.now(ALMATY_ZONE);
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now(ALMATY_ZONE);
    }
}
