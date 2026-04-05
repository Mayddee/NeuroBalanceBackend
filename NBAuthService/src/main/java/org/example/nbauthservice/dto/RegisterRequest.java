//package org.example.nbauthservice.dto;
//
//import com.fasterxml.jackson.annotation.JsonProperty;
//import io.swagger.v3.oas.annotations.media.Schema;
//import jakarta.validation.constraints.Email;
//import jakarta.validation.constraints.NotNull;
//import lombok.Data;
//import org.hibernate.validator.constraints.Length;
//
//@Data
//@Schema(description = "Registration request")
//public class RegisterRequest {
//
//    @Schema(description = "Name", example = "John Doe")
//    @NotNull(message = "Name cannot be null")
//    @Length(min = 2, max = 255, message = "Name must be between 2 and 255 characters")
//    private String name;
//
//    @Schema(description = "Email", example = "john@example.com")
//    @NotNull(message = "Email cannot be null")
//    @Email(message = "Email must be valid")
//    @Length(max = 255, message = "Email must be less than 255 characters")
//    private String email;
//
//    @Schema(description = "Username", example = "johndoe")
//    @NotNull(message = "Username cannot be null")
//    @Length(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
//    private String username;
//
//    @Schema(description = "Phone (optional)", example = "+77079062051")
//    @Length(max = 20, message = "Phone must be less than 20 characters")
//    private String phone;
//
//    @Schema(description = "Password", example = "SecurePass123")
//    @NotNull(message = "Password cannot be null")
//    @Length(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
//    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
//    private String password;
//}
package org.example.nbauthservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.time.LocalDate;

@Data
@Schema(description = "Registration request with optional onboarding")
public class RegisterRequest {

    // ========== ОБЯЗАТЕЛЬНЫЕ ПОЛЯ ==========

    @Schema(description = "Name", example = "John Doe")
    @NotNull(message = "Name cannot be null")
    @Length(min = 2, max = 255, message = "Name must be between 2 and 255 characters")
    private String name;

    @Schema(description = "Email", example = "john@example.com")
    @NotNull(message = "Email cannot be null")
    @Email(message = "Email must be valid")
    @Length(max = 255, message = "Email must be less than 255 characters")
    private String email;

    @Schema(description = "Username", example = "johndoe")
    @NotNull(message = "Username cannot be null")
    @Length(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @Schema(description = "Password", example = "SecurePass123")
    @NotNull(message = "Password cannot be null")
    @Length(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    @Schema(description = "Phone (optional)", example = "+77079062051")
    @Length(max = 20, message = "Phone must be less than 20 characters")
    private String phone;

    // ========== ОПЦИОНАЛЬНЫЕ ПОЛЯ ОНБОРДИНГА ==========

    @Schema(description = "Sex (optional for onboarding)", example = "MALE", allowableValues = {"MALE", "FEMALE", "OTHER"})
    private String sex;

    @Schema(description = "Height in cm (optional for onboarding)", example = "175")
    @Min(value = 50, message = "Height must be at least 50 cm")
    @Max(value = 300, message = "Height must be at most 300 cm")
    private Integer heightCm;

    @Schema(description = "Weight in kg (optional for onboarding)", example = "70")
    @Min(value = 20, message = "Weight must be at least 20 kg")
    @Max(value = 500, message = "Weight must be at most 500 kg")
    private Integer weightKg;

    @Schema(description = "Birth date (optional for onboarding)", example = "1990-01-15")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthDate;

    @Schema(description = "Character ID (optional for onboarding)", example = "1")
    @Min(value = 1, message = "Character ID must be at least 1")
    private Integer characterId;

    @Schema(description = "Data consent (optional for onboarding)", example = "true")
    private Boolean dataConsent;

    /**
     * Проверяет заполнены ли все поля онбординга
     */
    public boolean hasOnboardingData() {
        return sex != null
                && heightCm != null
                && weightKg != null
                && birthDate != null
                && characterId != null
                && dataConsent != null
                && dataConsent; // Должен согласиться на сбор данных
    }
}