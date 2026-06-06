package org.example.nbcheckinservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.Period;
import java.util.Map;
import java.util.Optional;

/**
 * HTTP client for NBAuthService.
 * Used to fetch the authenticated user's onboarding data (age, sex)
 * for personalizing ML recommendations.
 *
 * Calls GET /api/v1/onboarding with the user's own JWT token
 * — no service account or new auth endpoints needed.
 */
@Component
@Slf4j
public class AuthServiceClient {

    private final RestTemplate restTemplate;
    private final String authServiceUrl;

    public AuthServiceClient(
            RestTemplate restTemplate,
            @Value("${auth.service.url:http://localhost:8081}") String authServiceUrl
    ) {
        this.restTemplate = restTemplate;
        this.authServiceUrl = authServiceUrl;
    }

    /**
     * Fetches the current user's onboarding data from NBAuthService.
     * The JWT is forwarded so NBAuthService identifies the user from the token.
     *
     * @param authorizationHeader  full "Bearer <token>" header from the original request
     * @return Optional with parsed onboarding data, or empty on error / not found
     */
    public Optional<OnboardingData> getMyOnboarding(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            log.debug("No auth header — skipping onboarding enrichment");
            return Optional.empty();
        }

        try {
            String url = authServiceUrl + "/api/v1/onboarding";

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", authorizationHeader);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                OnboardingData data = parse(response.getBody());
                log.debug("Onboarding fetched from auth service: age={}, gender={}", data.age(), data.gender());
                return Optional.of(data);
            }

        } catch (Exception e) {
            // Graceful degradation — ML still works with defaults if auth service is down
            log.warn("Could not fetch onboarding from auth service: {}", e.getMessage());
        }
        return Optional.empty();
    }

    // ─── Parsing ────────────────────────────────────────────────

    private OnboardingData parse(Map<String, Object> body) {
        String gender = parseGender(body.get("sex"));
        int age = parseAge(body.get("birthDate"));
        return new OnboardingData(age, gender);
    }

    private String parseGender(Object sexRaw) {
        if (sexRaw == null) return "Male";
        String sex = sexRaw.toString().toUpperCase();
        return sex.contains("FEMALE") ? "Female" : "Male";
    }

    private int parseAge(Object birthDateRaw) {
        if (birthDateRaw == null) return 25;
        try {
            LocalDate birthDate = LocalDate.parse(birthDateRaw.toString());
            int age = Period.between(birthDate, LocalDate.now()).getYears();
            // Clamp to ML model valid range [18, 100]
            return Math.max(18, Math.min(age, 100));
        } catch (Exception e) {
            log.warn("Could not parse birthDate '{}': {}", birthDateRaw, e.getMessage());
            return 25;
        }
    }

    /**
     * Parsed onboarding fields relevant to ML recommendations.
     * Only contains what the ML model actually uses.
     */
    public record OnboardingData(int age, String gender) {}
}
