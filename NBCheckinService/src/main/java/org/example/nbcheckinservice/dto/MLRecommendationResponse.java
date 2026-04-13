package org.example.nbcheckinservice.dto;

import lombok.Data;
import java.util.List;

@Data
public class MLRecommendationResponse {
    private String status;
    private Double cognitiveScore;
    private String summary;
    private List<Recommendation> recommendations;
    private Double totalPotentialImprovement;
    private String timestamp;

    @Data
    public static class Recommendation {
        private String type;
        private String title;
        private String emoji;
        private String message;
        private String impactText;
        private Double predictedImprovement;
        private String priority;
        private Double baseline;
        private Double recommendedTarget;
        private List<String> actions;
        private String scientificBasis;
    }
}