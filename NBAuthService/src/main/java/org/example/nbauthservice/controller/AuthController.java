package org.example.nbauthservice.controller;

import lombok.RequiredArgsConstructor;
import org.example.nbauthservice.dto.JwtRequest;
import org.example.nbauthservice.dto.JwtResponse;
import org.example.nbauthservice.dto.RegisterRequest;
import org.example.nbauthservice.entity.User;
import org.example.nbauthservice.repository.UserRepository;
import org.example.nbauthservice.service.AuthService;
import org.example.nbauthservice.service.EmailVerificationService;
import org.example.nbauthservice.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Validated
public class AuthController {
    private final AuthService authService;
    private final UserService userService;
    private final UserRepository userRepository;
    private final EmailVerificationService emailVerificationService;



    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Validated @RequestBody RegisterRequest request) {
        try {
            User user = new User();
            user.setName(request.getName());
            user.setEmail(request.getEmail());
            user.setUsername(request.getUsername());
            user.setPhone(request.getPhone());
            user.setPassword(request.getPassword());

            user = userService.create(user);

            return ResponseEntity.ok(Map.of(
                    "message", "Registration successful",
                    "email", user.getEmail(),
                    "info", "Verification link sent to your email"
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }


    @PostMapping("/login")
    public JwtResponse login(@Validated @RequestBody JwtRequest request) {
        return authService.login(request);
    }

    @GetMapping("/verify-email")
    public ResponseEntity<String> verifyEmail(@RequestParam String token) {
        String result = emailVerificationService.verifyEmail(token);
        return ResponseEntity.ok(result);
    }


    @PostMapping("/verify-user-manual")
    public ResponseEntity<String> verifyUserManual(@RequestParam String username) {
        try {
            userService.verifyUserManually(username);
            return ResponseEntity.ok("User " + username + " verified successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/refresh")
    public JwtResponse refresh(@RequestBody String refreshToken) {
        return authService.refreshToken(refreshToken);
    }
}

