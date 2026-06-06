package org.example.nbcheckinservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nbcheckinservice.dto.MLMetricsRequest;
import org.example.nbcheckinservice.dto.MLRecommendationResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class MLService {

    private final RestTemplate restTemplate;

    @Value("${ml.service.url:http://localhost:5001}")
    private String mlServiceUrl;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

    /**
     * Called by Java backend internaly — passes user_id so Python caches the result.
     * Python's /recommend/top3 computes with REAL params (from buildRequest),
     * caches under user_id, returns result.
     * Frontend calling /recommend/top3 with same user_id then gets the cached real result.
     */
    public MLRecommendationResponse getTop3Recommendations(MLMetricsRequest request, Long userId) {
        try {
            String url = mlServiceUrl + "/recommend/top3";

            Map<String, Object> payload = buildPayload(request);
            if (userId != null) {
                payload.put("user_id", userId);     // Python caches result under this user_id
                payload.put("internal", true);       // tells Python to always recompute (real data, not cache hit)
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Accept-Charset", "UTF-8");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

            log.info("ML Service call for user {}: {}", userId, url);

            // Get raw String to deserialize manually with snake_case ObjectMapper.
            // RestTemplate's default Jackson uses camelCase so Python's snake_case fields
            // (cognitive_score, predicted_improvement, etc.) would all be null otherwise.
            ResponseEntity<String> rawResponse = restTemplate.exchange(
                    url, HttpMethod.POST, entity, String.class
            );

            if (rawResponse.getStatusCode() == HttpStatus.OK && rawResponse.getBody() != null) {
                String rawJson = rawResponse.getBody();
                // Python returns "total_potential" but Java field is totalPotentialImprovement
                // (snake_case ObjectMapper maps it as total_potential_improvement) → fix the key
                String normalizedJson = rawJson.replace(
                        "\"total_potential\":", "\"total_potential_improvement\":");
                MLRecommendationResponse response = objectMapper.readValue(
                        normalizedJson, MLRecommendationResponse.class);
                log.info("ML recommendations received for user {} (score={})", userId,
                        response.getCognitiveScore());
                return response;
            } else {
                throw new RuntimeException("ML service returned empty response");
            }

        } catch (Exception e) {
            log.error("Error calling ML Service for user {}: {}", userId, e.getMessage());
            throw new RuntimeException("Failed to get recommendations: " + e.getMessage());
        }
    }

    /** Backward-compatible overload (no user_id — Python just computes, no cache) */
    public MLRecommendationResponse getTop3Recommendations(MLMetricsRequest request) {
        return getTop3Recommendations(request, null);
    }

    public MLRecommendationResponse getSingleBestRecommendation(MLMetricsRequest request) {
        try {
            String url = mlServiceUrl + "/recommend/single";

            Map<String, Object> payload = buildPayload(request);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Accept-Charset", "UTF-8");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

            ResponseEntity<String> rawResponse = restTemplate.exchange(
                    url, HttpMethod.POST, entity, String.class
            );

            if (rawResponse.getStatusCode() == HttpStatus.OK && rawResponse.getBody() != null) {
                String normalizedJson = rawResponse.getBody().replace(
                        "\"total_potential\":", "\"total_potential_improvement\":");
                MLRecommendationResponse response = objectMapper.readValue(
                        normalizedJson, MLRecommendationResponse.class);
                log.info("Single best recommendation received (score={})", response.getCognitiveScore());
                return response;
            } else {
                throw new RuntimeException("ML service returned empty response");
            }

        } catch (Exception e) {
            log.error("❌ Error calling ML Service: {}", e.getMessage());
            throw new RuntimeException("Failed to get recommendation: " + e.getMessage());
        }
    }

    private Map<String, Object> buildPayload(MLMetricsRequest request) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("sleep_duration", request.getSleepDuration());
        payload.put("stress_level", request.getStressLevel());
        payload.put("daily_screen_time", request.getDailyScreenTime());
        payload.put("exercise_frequency", request.getExerciseFrequency());
        payload.put("caffeine_intake", request.getCaffeineIntake());
        payload.put("reaction_time", request.getReactionTime());
        payload.put("memory_test_score", request.getMemoryTestScore());
        payload.put("age", request.getAge());
        payload.put("gender", request.getGender());
        payload.put("diet_type", request.getDietType());
        return payload;
    }
}