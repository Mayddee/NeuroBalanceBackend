package org.example.nbcheckinservice.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class SleepLoggedApplicationEvent {
    private final Long userId;
    private final LocalDate sleepDate;
    private final String eventType;
}
