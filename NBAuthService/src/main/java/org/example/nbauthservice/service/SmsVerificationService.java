package org.example.nbauthservice.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.nbauthservice.entity.User;
import org.example.nbauthservice.exception.EntityNotFoundException;
import org.example.nbauthservice.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class SmsVerificationService {

    private final UserRepository userRepository;
    private final SmsSender smsSender;

    private final Map<String, String> phoneCodeCache = new ConcurrentHashMap<>();


    public void sendVerificationCode(User user) {
        String code = String.format("%06d", new Random().nextInt(999999)); // шестизначный код
        phoneCodeCache.put(user.getPhone(), code);

        String message = "Your MessageMe verification code: " + code;
        smsSender.sendSms(user.getPhone(), message);

        System.out.println("Verification code for " + user.getPhone() + ": " + code);
    }

    /**
     * Проверяем код и активируем аккаунт
     */
    @Transactional
    public boolean verifyCode(String phone, String code) {
        String cachedCode = phoneCodeCache.get(phone);

        if (cachedCode != null && cachedCode.equals(code)) {
            User user = userRepository.findByPhone(phone)
                    .orElseThrow(() -> new IllegalArgumentException("User not found with phone " + phone));
            user.setPhoneVerified(true);
            userRepository.save(user);
            phoneCodeCache.remove(phone);
            return true;
        }
        return false;
    }
}