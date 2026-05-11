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
 * Kafka consumer: подписывается на топик checkin.created.
 *
 * После создания чекина асинхронно вычисляет и сохраняет
 * M-Rest, M-Ready, M-Balance без блокировки HTTP-ответа пользователю.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class HealthMetricsKafkaConsumer {

    private final HealthMetricsService healthMetricsService;

    @KafkaListener(
            topics = "${kafka.topics.checkin-created:checkin.created}",
            groupId = "${spring.kafka.consumer.group-id:health-metrics-consumer-group}"
    )
    public void handleCheckInCreated(
            @Payload CheckInEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset
    ) {
        log.info("Received CheckInEvent: topic={}, partition={}, offset={}, userId={}, date={}",
                topic, partition, offset, event.getUserId(), event.getCheckInDate());
        try {
            LocalDate date = LocalDate.parse(event.getCheckInDate());
            healthMetricsService.calculateAndSave(event.getUserId(), date)
                    .ifPresentOrElse(
                            m -> log.info("Health metrics saved for user {}: mRest={}, mReady={}, mBalance={}",
                                    event.getUserId(), m.getMRest(), m.getMReady(), m.getMBalance()),
                            () -> log.warn("No check-in found for user {} on {} — metrics skipped",
                                    event.getUserId(), event.getCheckInDate())
                    );
        } catch (Exception e) {
            log.error("Error processing CheckInEvent for user {} on {}: {}",
                    event.getUserId(), event.getCheckInDate(), e.getMessage(), e);
        }
    }
}
