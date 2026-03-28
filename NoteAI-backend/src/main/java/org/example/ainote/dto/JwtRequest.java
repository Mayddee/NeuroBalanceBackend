package org.example.ainote.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class JwtRequest {
    @NotNull(message="Username can not be null")
    private String username;

    @NotNull(message="Password can not be null")
    private String password;
}
