package com.rentease.service;

import com.rentease.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/** Sends the password reset email. Without a configured mail host it prints the link (DEV MODE). */
@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final ObjectProvider<JavaMailSender> mailSender;
    private final String frontendUrl;
    private final String from;
    private final String smtpUsername;
    private final long expirationMinutes;

    public MailService(ObjectProvider<JavaMailSender> mailSender,
                       @Value("${app.frontend-url}") String frontendUrl,
                       @Value("${app.mail.from:}") String from,
                       @Value("${spring.mail.username:}") String smtpUsername,
                       @Value("${app.password-reset.expiration-minutes}") long expirationMinutes) {
        this.mailSender = mailSender;
        this.frontendUrl = frontendUrl;
        this.from = from;
        this.smtpUsername = smtpUsername;
        this.expirationMinutes = expirationMinutes;
    }

    public void sendPasswordReset(User user, String rawToken) {
        String link = frontendUrl + "/reset-password?token=" + rawToken;

        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            log.warn("Email is not configured (DEV MODE). Password reset link for {}: {}", user.getEmail(), link);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            String sender1 = !from.isBlank() ? from : smtpUsername;
            if (!sender1.isBlank()) {
                message.setFrom(sender1);
            }
            message.setTo(user.getEmail());
            message.setSubject("RentEase password reset");
            message.setText("Hello " + user.getFullName() + ",\n\n"
                    + "We received a request to reset your RentEase password. "
                    + "Open this link to choose a new password (valid for " + expirationMinutes
                    + " minutes, one use only):\n\n" + link + "\n\n"
                    + "If you did not ask for this, you can ignore this email and your password stays the same.");
            sender.send(message);
        } catch (MailException ex) {
            log.error("Could not send the password reset email to {}: {}", user.getEmail(), ex.getMessage());
        }
    }
}
