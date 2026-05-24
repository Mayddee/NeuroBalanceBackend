package org.example.nbcheckinservice.dto;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class FlexibleLocalTimeDeserializer extends StdDeserializer<LocalTime> {

    private static final ZoneId ALMATY_ZONE = ZoneId.of("Asia/Almaty");

    public FlexibleLocalTimeDeserializer() {
        super(LocalTime.class);
    }

    @Override
    public LocalTime deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
        String value = p.getText().trim();
        if (value.isEmpty()) return null;

        // Try HH:mm or HH:mm:ss or HH:mm:ss.SSS
        try {
            return LocalTime.parse(value, DateTimeFormatter.ofPattern("HH:mm[:ss[.SSS]]"));
        } catch (DateTimeParseException ignored) {}

        // Try full ISO offset datetime: "2026-03-30T23:00:00Z", "2026-03-30T23:00:00+05:00"
        try {
            return OffsetDateTime.parse(value)
                    .atZoneSameInstant(ALMATY_ZONE)
                    .toLocalTime();
        } catch (DateTimeParseException ignored) {}

        // Try ISO local datetime without offset: "2026-03-30T23:00:00"
        try {
            return LocalTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException ignored) {}

        throw ctxt.instantiationException(LocalTime.class, "Cannot parse LocalTime from: " + value);
    }
}