package org.example.nbauthservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JwtResponse {
    private Long id;
    private String username;
    private String name;
    private String email;
    private String token;
    private String refreshToken;
    private boolean isOnboarded; // ✅ НОВОЕ
}