package org.example.ainote.dto;

import lombok.Data;

@Data
public class JwtResponse {
    private Long id;
    private String name;
    private String username;
    private String accessToken;
    private String refreshToken;
}
