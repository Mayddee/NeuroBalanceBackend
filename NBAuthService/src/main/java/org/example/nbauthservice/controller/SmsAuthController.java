package org.example.nbauthservice.controller;


import lombok.RequiredArgsConstructor;
import org.example.nbauthservice.entity.User;
import org.example.nbauthservice.repository.UserRepository;
import org.example.nbauthservice.service.SmsVerificationService;
import org.example.nbauthservice.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class SmsAuthController {

    private final SmsVerificationService smsVerificationService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;


    @PostMapping("/register-phone")
    public ResponseEntity<String> registerWithPhone(@RequestBody User user) {
        if (userRepository.findByPhone(user.getPhone()).isPresent()) {
            return ResponseEntity.badRequest().body("Phone already registered");
        }


        userService.create(user);
        smsVerificationService.sendVerificationCode(user);
        return ResponseEntity.ok("Verification code sent to " + user.getPhone());
    }

    @PostMapping("/verify-phone")
    public ResponseEntity<String> verifyPhone(@RequestParam String phone, @RequestParam String code) {
        boolean verified = smsVerificationService.verifyCode(phone, code);
        if (verified) {
            return ResponseEntity.ok("Phone verified successfully");
        } else {
            return ResponseEntity.badRequest().body("Invalid verification code");
        }
    }
}