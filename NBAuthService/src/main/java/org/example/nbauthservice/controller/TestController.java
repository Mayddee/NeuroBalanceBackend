package org.example.nbauthservice.controller;

import lombok.RequiredArgsConstructor;
import org.example.nbauthservice.service.SmsSender;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/test")
public class TestController {
    private final SmsSender smsSender;

    @GetMapping("/sms")
    public String sendTestSms(@RequestParam String to) {
        smsSender.sendSms(to, "Hello! This is a test message from MessageMe app.");
        return "SMS sent to " + to;
    }
}
