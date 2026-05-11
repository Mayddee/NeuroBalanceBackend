package org.example.nbcheckinservice.kafka;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SleepLogEvent {
    private Long userId;
    private String sleepDate; // "yyyy-MM-dd"
    private String eventType; // "CREATED" | "UPDATED"
}
