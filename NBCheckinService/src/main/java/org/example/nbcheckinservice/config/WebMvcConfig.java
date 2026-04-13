package org.example.nbcheckinservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Глобальные конвертеры для PathVariable и RequestParam.
 * Автоматически обрезает пробелы в датах (решает ошибку " 2026-04-13").
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(String.class, LocalDate.class, source -> {
            if (source == null) return null;
            String trimmed = source.trim();
            if (trimmed.isEmpty()) return null;
            try {
                return LocalDate.parse(trimmed, DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException(
                        "Неверный формат даты: '" + trimmed + "'. Ожидается yyyy-MM-dd"
                );
            }
        });

        registry.addConverter(String.class, LocalDateTime.class, source -> {
            if (source == null) return null;
            String trimmed = source.trim();
            if (trimmed.isEmpty()) return null;
            try {
                return LocalDateTime.parse(trimmed, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException(
                        "Неверный формат даты-времени: '" + trimmed + "'. Ожидается yyyy-MM-ddTHH:mm:ss"
                );
            }
        });
    }
}
