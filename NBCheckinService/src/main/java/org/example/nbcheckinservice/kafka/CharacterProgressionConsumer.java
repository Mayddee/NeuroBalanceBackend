package org.example.nbcheckinservice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nbcheckinservice.service.RewardService;
import org.example.nbcheckinservice.service.UserCharacterService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Listens to game.completed and checkin.created events.
 * After each activity, automatically checks whether the user has unlocked
 * a pending level-up (XP near threshold + streak condition now met).
 * Also re-checks reward eligibility (streak-based rewards).
 *
 * Uses a separate consumer group so it does not interfere with
 * the existing health-metrics-consumer-group listeners.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CharacterProgressionConsumer {

    private final UserCharacterService characterService;
    private final RewardService rewardService;

    @KafkaListener(
            topics = "${kafka.topics.game-completed:game.completed}",
            groupId = "character-progression-consumer-group"
    )
    public void onGameCompleted(GameCompletedEvent event) {
        handleProgressionCheck(event.getUserId(), "game.completed");
    }

    @KafkaListener(
            topics = "${kafka.topics.checkin-created:checkin.created}",
            groupId = "character-progression-consumer-group"
    )
    public void onCheckInCreated(CheckInEvent event) {
        handleProgressionCheck(event.getUserId(), "checkin.created");
    }

    private void handleProgressionCheck(Long userId, String source) {
        try {
            characterService.checkAndAutoLevelUp(userId);
            rewardService.checkAndUnlockRewards(userId);
            log.debug("Progression check completed for user {} (source={})", userId, source);
        } catch (Exception e) {
            log.warn("Progression check failed for user {} (source={}, non-critical): {}",
                    userId, source, e.getMessage());
        }
    }
}