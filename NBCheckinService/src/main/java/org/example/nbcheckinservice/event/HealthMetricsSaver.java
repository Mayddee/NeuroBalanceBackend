package org.example.nbcheckinservice.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nbcheckinservice.service.HealthMetricsService;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

/**
 * Saves health metrics AFTER the check-in transaction commits.
 * Runs before Kafka publish (Order=1) so metrics are in DB before the consumer fires.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(1)
public class HealthMetricsSaver {

    private final HealthMetricsService healthMetricsService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCheckInCreated(CheckInCreatedApplicationEvent event) {
        log.info("TX committed — saving health metrics for user {}, date {}",
                event.getUserId(), event.getCheckInDate());
        try {
            healthMetricsService.calculateAndSave(event.getUserId(), event.getCheckInDate())
                    .ifPresentOrElse(
                            m -> log.info("Health metrics saved: user={}, date={}, mRest={}, mReady={}, mBalance={}, overall={}",
                                    event.getUserId(), event.getCheckInDate(),
                                    m.getMRest(), m.getMReady(), m.getMBalance(), m.getOverallWellnessScore()),
                            () -> log.warn("Check-in not found when saving health metrics: user={}, date={}",
                                    event.getUserId(), event.getCheckInDate())
                    );
        } catch (Exception e) {
            log.error("Failed to save health metrics for user {} on {}: {}",
                    event.getUserId(), event.getCheckInDate(), e.getMessage(), e);
        }
    }
}
