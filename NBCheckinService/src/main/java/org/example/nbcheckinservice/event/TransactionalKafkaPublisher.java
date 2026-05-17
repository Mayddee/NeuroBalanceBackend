package org.example.nbcheckinservice.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nbcheckinservice.kafka.KafkaProducerService;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

/**
 * Publishes Kafka events AFTER the DB transaction commits (Order=2, runs after HealthMetricsSaver).
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(2)
public class TransactionalKafkaPublisher {

    private final KafkaProducerService kafkaProducerService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCheckInCreated(CheckInCreatedApplicationEvent event) {
        log.info("TX committed — publishing CheckInEvent for user {}, date {}",
                event.getUserId(), event.getCheckInDate());
        kafkaProducerService.publishCheckInCreated(event.getUserId(), event.getCheckInDate());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSleepLogged(SleepLoggedApplicationEvent event) {
        log.info("TX committed — publishing SleepLogEvent for user {}, date {}",
                event.getUserId(), event.getSleepDate());
        kafkaProducerService.publishSleepLogged(
                event.getUserId(), event.getSleepDate(), event.getEventType());
    }
}
