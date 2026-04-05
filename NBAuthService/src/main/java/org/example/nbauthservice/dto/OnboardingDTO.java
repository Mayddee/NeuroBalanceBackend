package org.example.nbauthservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.example.nbauthservice.dto.validation.OnCreate;
import org.example.nbauthservice.dto.validation.OnUpdate;

import java.time.LocalDate;

@Data
@Schema(description = "User onboarding data")
public class OnboardingDTO {

    @Schema(description = "Onboarding ID", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Schema(description = "User ID", accessMode = Schema.AccessMode.READ_ONLY)
    private Long userId;

    @Schema(description = "Sex", example = "MALE", allowableValues = {"MALE", "FEMALE", "OTHER"})
    @NotNull(message = "Sex cannot be null", groups = {OnCreate.class})
    private String sex;

    @Schema(description = "Height in cm", example = "175")
    @NotNull(message = "Height cannot be null", groups = {OnCreate.class})
    @Min(value = 50, message = "Height must be at least 50 cm")
    @Max(value = 300, message = "Height must be at most 300 cm")
    private Integer heightCm;

    @Schema(description = "Weight in kg", example = "70")
    @NotNull(message = "Weight cannot be null", groups = {OnCreate.class})
    @Min(value = 20, message = "Weight must be at least 20 kg")
    @Max(value = 500, message = "Weight must be at most 500 kg")
    private Integer weightKg;

    @Schema(description = "Birth date", example = "1990-01-15")
    @NotNull(message = "Birth date cannot be null", groups = {OnCreate.class})
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthDate;

    @Schema(description = "Character ID", example = "1")
    @NotNull(message = "Character ID cannot be null", groups = {OnCreate.class})
    @Min(value = 1, message = "Character ID must be at least 1")
    private Integer characterId;

    @Schema(description = "Data consent", example = "true")
    @NotNull(message = "Data consent cannot be null", groups = {OnCreate.class})
    private Boolean dataConsent;

    @Schema(description = "Is onboarding completed", accessMode = Schema.AccessMode.READ_ONLY)
    private Boolean isCompleted;
}