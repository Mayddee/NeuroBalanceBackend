package org.example.nbauthservice.controller;

import lombok.RequiredArgsConstructor;
import org.example.nbauthservice.dto.JwtRequest;
import org.example.nbauthservice.dto.JwtResponse;
import org.example.nbauthservice.dto.RegisterRequest;
import org.example.nbauthservice.entity.User;
import org.example.nbauthservice.entity.UserOnboarding;
import org.example.nbauthservice.repository.UserOnboardingRepository;
import org.example.nbauthservice.repository.UserRepository;
import org.example.nbauthservice.service.AuthService;
import org.example.nbauthservice.service.EmailVerificationService;
import org.example.nbauthservice.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Validated
public class AuthController {
    private final AuthService authService;
    private final UserService userService;
    private final UserRepository userRepository;
    private final UserOnboardingRepository onboardingRepository;
    private final EmailVerificationService emailVerificationService;

    /**
     * Регистрация с опциональным онбордингом

     * Если поля онбординга не указаны, регистрация пройдет без онбординга, isOnboarded = false.
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Validated @RequestBody RegisterRequest request) {
        try {
            // Создать пользователя
            User user = new User();
            user.setName(request.getName());
            user.setEmail(request.getEmail());
            user.setUsername(request.getUsername());
            user.setPhone(request.getPhone());
            user.setPassword(request.getPassword());

            user = userService.create(user);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Registration successful");
            response.put("userId", user.getId());
            response.put("username", user.getUsername());
            response.put("email", user.getEmail());

            // ✅ НОВОЕ: Проверить есть ли данные онбординга
            if (request.hasOnboardingData()) {
                // Сохранить онбординг
                UserOnboarding onboarding = UserOnboarding.builder()
                        .user(user)
                        .sex(request.getSex())
                        .heightCm(request.getHeightCm())
                        .weightKg(request.getWeightKg())
                        .birthDate(request.getBirthDate())
                        .characterId(request.getCharacterId())
                        .dataConsent(request.getDataConsent())
                        .isCompleted(true)
                        .build();

                onboardingRepository.save(onboarding);

                // Обновить флаг onboarded
                user.setOnboarded(true);
                userRepository.save(user);

                response.put("isOnboarded", true);
                response.put("onboardingCompleted", true);
            } else {
                response.put("isOnboarded", false);
                response.put("onboardingCompleted", false);
                response.put("info", "You can complete onboarding later via /api/v1/onboarding");
            }

            return ResponseEntity.ok(response);
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

//package org.example.nbauthservice.controller;
//
//import lombok.RequiredArgsConstructor;
//import org.example.nbauthservice.dto.JwtRequest;
//import org.example.nbauthservice.dto.JwtResponse;
//import org.example.nbauthservice.dto.RegisterRequest;
//import org.example.nbauthservice.entity.User;
//import org.example.nbauthservice.repository.UserRepository;
//import org.example.nbauthservice.service.AuthService;
//import org.example.nbauthservice.service.EmailVerificationService;
//import org.example.nbauthservice.service.UserService;
//import org.springframework.http.ResponseEntity;
//import org.springframework.validation.annotation.Validated;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.Map;
//
//@RestController
//@RequestMapping("/api/v1/auth")
//@RequiredArgsConstructor
//@Validated
//public class AuthController {
//    private final AuthService authService;
//    private final UserService userService;
//    private final UserRepository userRepository;
//    private final EmailVerificationService emailVerificationService;
//
//
//
//    @PostMapping("/register")
//    public ResponseEntity<Map<String, String>> register(@Validated @RequestBody RegisterRequest request) {
//        try {
//            User user = new User();
//            user.setName(request.getName());
//            user.setEmail(request.getEmail());
//            user.setUsername(request.getUsername());
//            user.setPhone(request.getPhone());
//            user.setPassword(request.getPassword());
//
//            user = userService.create(user);
//
//            return ResponseEntity.ok(Map.of(
//                    "message", "Registration successful",
//                    "email", user.getEmail(),
//                    "info", "Verification link sent to your email"
//            ));
//        } catch (IllegalArgumentException e) {
//            return ResponseEntity.badRequest()
//                    .body(Map.of("error", e.getMessage()));
//        }
//    }
//
//
//    @PostMapping("/login")
//    public JwtResponse login(@Validated @RequestBody JwtRequest request) {
//        return authService.login(request);
//    }
//
//    @GetMapping("/verify-email")
//    public ResponseEntity<String> verifyEmail(@RequestParam String token) {
//        String result = emailVerificationService.verifyEmail(token);
//        return ResponseEntity.ok(result);
//    }
//
//
//    @PostMapping("/verify-user-manual")
//    public ResponseEntity<String> verifyUserManual(@RequestParam String username) {
//        try {
//            userService.verifyUserManually(username);
//            return ResponseEntity.ok("User " + username + " verified successfully");
//        } catch (Exception e) {
//            return ResponseEntity.badRequest().body(e.getMessage());
//        }
//    }
//
//    @PostMapping("/refresh")
//    public JwtResponse refresh(@RequestBody String refreshToken) {
//        return authService.refreshToken(refreshToken);
//    }
//}
//
