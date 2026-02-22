
package org.example.nbauthservice.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.nbauthservice.entity.User;
import org.example.nbauthservice.entity.VerificationToken;
import org.example.nbauthservice.repository.UserRepository;
import org.example.nbauthservice.repository.VerificationTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final JavaMailSender mailSender;
    private final VerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;

    @Value("${app.auth.base-url:http://localhost:8081}")
    private String authBaseUrl;

    public void sendVerificationEmail(User user) {
        String token = UUID.randomUUID().toString();

        // Если уже есть токен для юзера → обновим его вместо вставки нового
        Optional<VerificationToken> existing = tokenRepository.findByUserId(user.getId());
        if (existing.isPresent()) {
            VerificationToken update = existing.get();
            update.setToken(token);
            update.setExpiryDate(LocalDateTime.now().plusHours(1));
            tokenRepository.save(update);

            // consequence: старый токен станет невалиден, новый будет действовать 1 час
        } else {
            VerificationToken newToken = VerificationToken.builder()
                    .token(token)
                    .user(user)
                    .expiryDate(LocalDateTime.now().plusHours(1))
                    .build();
            tokenRepository.save(newToken);
        }

        String verificationUrl = authBaseUrl + "/api/v1/auth/verify-email?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(user.getEmail());
        message.setSubject("Verify your MessageMe account");
        message.setText("Hello %s,\n\nPlease click the link to verify:\n%s\n\nExpires in 1 hour."
                .formatted(user.getName(), verificationUrl));

        mailSender.send(message);

        // consequence: если SMTP-конфиг неверный, упадёт exception и юзер не получит письмо
    }

    @Transactional
    public String verifyEmail(String token) {
        VerificationToken verificationToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid verification token"));

        if (verificationToken.isExpired()) {
            // consequence: 500 returned, user must request a new token
            throw new IllegalStateException("Token expired. Request a new verification link.");
        }

        User user = verificationToken.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);

        tokenRepository.delete(verificationToken);
        return "Email verified successfully for " + user.getEmail();
    }
}


