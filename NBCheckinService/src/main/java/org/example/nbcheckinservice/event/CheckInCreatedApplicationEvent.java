package org.example.nbcheckinservice.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class CheckInCreatedApplicationEvent {
    private final Long userId;
    private final LocalDate checkInDate;
}
