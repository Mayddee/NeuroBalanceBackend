package org.example.nbcheckinservice.kafka;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Kafka event, публикуемый после успешного создания DailyCheckIn.
 *
 * checkInDate хранится как String ("yyyy-MM-dd") — чтобы избежать
 * проблем с сериализацией LocalDate в Kafka JSON.
 * Consumer парсит обратно через LocalDate.parse(event.getCheckInDate()).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckInEvent {
    private Long userId;
    private String checkInDate; // "yyyy-MM-dd"
}
