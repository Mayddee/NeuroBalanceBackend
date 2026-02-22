package org.example.nbauthservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
@Schema(description = "Registration request")
public class RegisterRequest {

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

    @Schema(description = "Phone (optional)", example = "+77079062051")
    @Length(max = 20, message = "Phone must be less than 20 characters")
    private String phone;

    @Schema(description = "Password", example = "SecurePass123")
    @NotNull(message = "Password cannot be null")
    @Length(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;
}
