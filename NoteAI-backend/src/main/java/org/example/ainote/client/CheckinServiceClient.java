package org.example.ainote.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * HTTP client that notifies NBCheckinService when a note is written.
 * The call is @Async (fire-and-forget) so note creation never fails
 * if the checkin service is temporarily unavailable.
 */
@Component
@Slf4j
public class CheckinServiceClient {

    private final RestTemplate restTemplate;
    private final String checkinServiceUrl;

    public CheckinServiceClient(
            RestTemplate restTemplate,
            @Value("${checkin.service.url:http://localhost:8082}") String checkinServiceUrl
    ) {
        this.restTemplate = restTemplate;
        this.checkinServiceUrl = checkinServiceUrl;
    }

    /**
     * Asynchronously marks the WRITE_NOTE daily task as completed
     * in NBCheckinService by forwarding the user's JWT token.
     *
     * Endpoint called: POST /api/v1/tasks/note-written
     *
     * @param authorizationHeader full "Bearer <token>" header from the original request
     */
    @Async
    public void notifyNoteWritten(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            log.warn("No Authorization header — skipping WRITE_NOTE task notification");
            return;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", authorizationHeader);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            String url = checkinServiceUrl + "/api/v1/tasks/note-written";

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            log.info("WRITE_NOTE task notified to checkin service → HTTP {}", response.getStatusCode());
        } catch (Exception e) {
            // Graceful degradation — note creation must not fail because of this
            log.warn("Failed to notify checkin service about WRITE_NOTE task: {}", e.getMessage());
        }
    }
}
