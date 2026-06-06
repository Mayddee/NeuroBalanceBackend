package org.example.nbcheckinservice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nbcheckinservice.service.MLRecommendationCacheService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Listens to existing Kafka topics and triggers async ML recommendation refresh
 * whenever meaningful user data changes.
 *
 * Topics consumed (all pre-existing — NO new topics added):
 *   checkin.created  — daily check-in completed (sleep, stress, energy, exercise)
 *   sleep.logged     — detailed sleep data logged (enriches M-Rest)
 *   game.completed   — cognitive game finished (updates memory/reaction proxy)
 *
 * For mood logs there is no Kafka event.
 * MLRecommendationCacheService handles that case via the 30-minute TTL:
 * the next GET /api/v1/ml/recommendations after the TTL expires will re-read
 * mood-influenced data (stress, energy from check-in) from the DB.
 *
 * Each listener uses its own consumer group so it does NOT interfere with
 * the existing health-metrics-consumer-group or character-progression-consumer-group.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MLRecommendationConsumer {

    private final MLRecommendationCacheService cacheService;

    /**
     * Triggered when a daily check-in is created.
     * Check-in data includes: sleep hours, stress level, energy, exercise — the most
     * important inputs for the ML model. This is the highest-priority trigger.
     */
    @KafkaListener(
            topics  = "${kafka.topics.checkin-created:checkin.created}",
            groupId = "ml-recommendation-consumer-group"
    )
    public void onCheckInCreated(CheckInEvent event) {
        if (event == null || event.getUserId() == null) return;
        log.info("ML refresh triggered by checkin.created for user {}", event.getUserId());
        cacheService.asyncRefresh(event.getUserId(), "checkin.created");
    }

    /**
     * Triggered when a sleep log is created or updated.
     * Sleep data enriches M-Rest calculations and the sleep_duration ML feature.
     */
    @KafkaListener(
            topics  = "${kafka.topics.sleep-logged:sleep.logged}",
            groupId = "ml-recommendation-consumer-group"
    )
    public void onSleepLogged(SleepLogEvent event) {
        if (event == null || event.getUserId() == null) return;
        log.info("ML refresh triggered by sleep.logged for user {}", event.getUserId());
        cacheService.asyncRefresh(event.getUserId(), "sleep.logged");
    }

    /**
     * Triggered when a cognitive game session is completed.
     * Game results update memory_test_score and reaction_time proxies.
     */
    @KafkaListener(
            topics  = "${kafka.topics.game-completed:game.completed}",
            groupId = "ml-recommendation-consumer-group"
    )
    public void onGameCompleted(GameCompletedEvent event) {
        if (event == null || event.getUserId() == null) return;
        log.info("ML refresh triggered by game.completed for user {}", event.getUserId());
        cacheService.asyncRefresh(event.getUserId(), "game.completed");
    }
}
