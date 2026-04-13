package org.example.nbcheckinservice.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MLMetricsRequest {

    @NotNull
    @DecimalMin("0.0")
    @DecimalMax("24.0")
    private Double sleepDuration;

    @NotNull
    @Min(1)
    @Max(10)
    private Integer stressLevel;

    @NotNull
    @DecimalMin("0.0")
    @DecimalMax("24.0")
    private Double dailyScreenTime;

    @NotNull
    @Min(0)
    @Max(7)
    private Integer exerciseFrequency;

    @NotNull
    @Min(0)
    @Max(10)
    private Integer caffeineIntake;

    @NotNull
    @Min(100)
    @Max(1000)
    private Integer reactionTime;

    @NotNull
    @Min(0)
    @Max(100)
    private Integer memoryTestScore;

    @NotNull
    @Min(18)
    @Max(100)
    private Integer age;

    @NotNull
    private String gender;

    @NotNull
    private String dietType;
}