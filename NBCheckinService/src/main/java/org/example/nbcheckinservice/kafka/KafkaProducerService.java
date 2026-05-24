package org.example.nbcheckinservice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Публикует события в Kafka.
 *
 * Kafka publish обёрнут в try-catch — если Kafka недоступна,
 * чекин всё равно успешно сохраняется (graceful degradation).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final ZoneId ALMATY_ZONE = ZoneId.of("Asia/Almaty");

    @Value("${kafka.topics.checkin-created:checkin.created}")
    private String checkinCreatedTopic;

    @Value("${kafka.topics.sleep-logged:sleep.logged}")
    private String sleepLoggedTopic;

    @Value("${kafka.topics.game-completed:game.completed}")
    private String gameCompletedTopic;

    @Value("${kafka.topics.character-leveled-up:character.leveled-up}")
    private String characterLeveledUpTopic;

    public void publishCheckInCreated(Long userId, LocalDate checkInDate) {
        try {
            // Передаём дату как String ("yyyy-MM-dd") — безопасная сериализация
            CheckInEvent event = new CheckInEvent(userId, checkInDate.toString());
            kafkaTemplate.send(checkinCreatedTopic, String.valueOf(userId), event);
            log.info("Published CheckInEvent to '{}': userId={}, date={}",
                    checkinCreatedTopic, userId, checkInDate);
        } catch (Exception e) {
            log.warn("Failed to publish CheckInEvent to Kafka (non-critical): {}", e.getMessage());
        }
    }

    public void publishSleepLogged(Long userId, LocalDate sleepDate, String eventType) {
        try {
            SleepLogEvent event = new SleepLogEvent(userId, sleepDate.toString(), eventType);
            kafkaTemplate.send(sleepLoggedTopic, String.valueOf(userId), event);
            log.info("Published SleepLogEvent to '{}': userId={}, date={}, type={}",
                    sleepLoggedTopic, userId, sleepDate, eventType);
        } catch (Exception e) {
            log.warn("Failed to publish SleepLogEvent to Kafka (non-critical): {}", e.getMessage());
        }
    }

    public void publishGameCompleted(Long userId, String gameType, String difficultyLevel,
                                     Boolean isWin, Integer xpEarned) {
        try {
            String playedAt = LocalDateTime.now(ALMATY_ZONE).toString();
            GameCompletedEvent event = new GameCompletedEvent(userId, gameType, difficultyLevel,
                    isWin, xpEarned, playedAt);
            kafkaTemplate.send(gameCompletedTopic, String.valueOf(userId), event);
            log.info("Published GameCompletedEvent to '{}': userId={}, game={}, xp={}",
                    gameCompletedTopic, userId, gameType, xpEarned);
        } catch (Exception e) {
            log.warn("Failed to publish GameCompletedEvent to Kafka (non-critical): {}", e.getMessage());
        }
    }

    public void publishLevelUp(Long userId, int oldLevel, int newLevel,
                               String characterType, String characterEmoji, int totalXp) {
        try {
            CharacterLeveledUpEvent event = new CharacterLeveledUpEvent(
                    userId, oldLevel, newLevel, characterType, characterEmoji, totalXp);
            kafkaTemplate.send(characterLeveledUpTopic, String.valueOf(userId), event);
            log.info("Published CharacterLeveledUpEvent to '{}': userId={}, {}→{}",
                    characterLeveledUpTopic, userId, oldLevel, newLevel);
        } catch (Exception e) {
            log.warn("Failed to publish CharacterLeveledUpEvent to Kafka (non-critical): {}", e.getMessage());
        }
    }
}
