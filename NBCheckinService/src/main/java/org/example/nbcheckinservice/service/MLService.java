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

    public MLRecommendationResponse getTop3Recommendations(MLMetricsRequest request) {
        try {
            String url = mlServiceUrl + "/recommend/top3";

            Map<String, Object> payload = buildPayload(request);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Accept-Charset", "UTF-8");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

            log.info("🔮 Calling ML Service: {}", url);

            ResponseEntity<MLRecommendationResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    MLRecommendationResponse.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                log.info("✅ Top-3 recommendations received");
                return response.getBody();
            } else {
                throw new RuntimeException("ML service returned empty response");
            }

        } catch (Exception e) {
            log.error("❌ Error calling ML Service: {}", e.getMessage());
            throw new RuntimeException("Failed to get recommendations: " + e.getMessage());
        }
    }

    public MLRecommendationResponse getSingleBestRecommendation(MLMetricsRequest request) {
        try {
            String url = mlServiceUrl + "/recommend/single";

            Map<String, Object> payload = buildPayload(request);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Accept-Charset", "UTF-8");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

            ResponseEntity<MLRecommendationResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    MLRecommendationResponse.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                log.info("✅ Single best recommendation received");
                return response.getBody();
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