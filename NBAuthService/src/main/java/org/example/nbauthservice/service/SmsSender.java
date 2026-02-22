package org.example.nbauthservice.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SmsSender {

    private final String accountSid;
    private final String authToken;
    private final String twilioPhoneNumber;

    public SmsSender(
            @Value("${twilio.account-sid}") String accountSid,
            @Value("${twilio.auth-token}") String authToken,
            @Value("${twilio.phone-number}") String twilioPhoneNumber
    ) {
        this.accountSid = accountSid;
        this.authToken = authToken;
        this.twilioPhoneNumber = twilioPhoneNumber;
        Twilio.init(accountSid, authToken);
    }

    public void sendSms(String toPhoneNumber, String messageBody) {
        Message.creator(
                new PhoneNumber(toPhoneNumber),
                new PhoneNumber(twilioPhoneNumber),
                messageBody
        ).create();
        System.out.println("SMS sent to " + toPhoneNumber);
    }
}