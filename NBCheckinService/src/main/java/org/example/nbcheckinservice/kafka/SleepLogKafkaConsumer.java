package org.example.nbcheckinservice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nbcheckinservice.service.HealthMetricsService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Kafka consumer: подписывается на sleep.logged.
 *
 * Когда пользователь добавляет/обновляет SleepLog,
 * пересчитывает M-Rest (улучшается точность: deep+REM sleep учитываются).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SleepLogKafkaConsumer {

    private final HealthMetricsService healthMetricsService;

    @KafkaListener(
            topics = "${kafka.topics.sleep-logged:sleep.logged}",
            groupId = "${spring.kafka.consumer.group-id:health-metrics-consumer-group}"
    )
    public void handleSleepLogged(
            @Payload SleepLogEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset
    ) {
        log.info("Received SleepLogEvent: topic={}, partition={}, offset={}, userId={}, date={}, type={}",
                topic, partition, offset, event.getUserId(), event.getSleepDate(), event.getEventType());
        try {
            LocalDate date = LocalDate.parse(event.getSleepDate());
            healthMetricsService.calculateAndSave(event.getUserId(), date)
                    .ifPresentOrElse(
                            m -> log.info("Health metrics recalculated after sleep log for user {}: mRest={}",
                                    event.getUserId(), m.getMRest()),
                            () -> log.info("No check-in for user {} on {} — health metrics skipped",
                                    event.getUserId(), event.getSleepDate())
                    );
        } catch (Exception e) {
            log.error("Error processing SleepLogEvent for user {} on {}: {}",
                    event.getUserId(), event.getSleepDate(), e.getMessage(), e);
        }
    }
}
