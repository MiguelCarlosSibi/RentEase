package com.rentease.service;

import com.rentease.dto.AuthResponse;
import com.rentease.dto.ChangePasswordRequest;
import com.rentease.dto.ResetPasswordRequest;
import com.rentease.dto.UserResponse;
import com.rentease.entity.PasswordResetToken;
import com.rentease.entity.User;
import com.rentease.entity.UserStatus;
import com.rentease.exception.ApiException;
import com.rentease.repository.PasswordResetTokenRepository;
import com.rentease.repository.UserRepository;
import com.rentease.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;

/** Change password (logged in) and forgot / reset password (by emailed link). */
@Service
public class PasswordService {

    private static final ZoneId MANILA = ZoneId.of("Asia/Manila");
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String FIELDS_MESSAGE = "Please correct the highlighted fields";

    private final UserRepository users;
    private final PasswordResetTokenRepository resetTokens;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final MailService mailService;
    private final long expirationMinutes;

    public PasswordService(UserRepository users,
                           PasswordResetTokenRepository resetTokens,
                           PasswordEncoder passwordEncoder,
                           JwtService jwtService,
                           MailService mailService,
                           @Value("${app.password-reset.expiration-minutes}") long expirationMinutes) {
        this.users = users;
        this.resetTokens = resetTokens;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.mailService = mailService;
        this.expirationMinutes = expirationMinutes;
    }

    // ------------------------------------------------------------------ change password

    /**
     * Verifies the current password, stores the new hash, and raises the token version so the old
     * password and every token issued before the change stop working. The caller receives a fresh token.
     */
    @Transactional
    public AuthResponse changePassword(Long userId, ChangePasswordRequest request) {
        if (!request.newPassword().equals(request.confirmNewPassword())) {
            throw fieldError("confirmNewPassword", "Passwords do not match");
        }

        User user = users.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw fieldError("currentPassword", "Current password is incorrect");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw fieldError("newPassword", "New password must be different from your current password");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setMustChangePassword(false); // a temporary password has now been replaced (FR006)
        user.incrementTokenVersion();
        users.save(user);
        resetTokens.deleteByUser(user); // any pending reset link is no longer needed

        return new AuthResponse(jwtService.generateToken(user), UserResponse.from(user));
    }

    // ------------------------------------------------------------------ forgot password

    /**
     * Creates a reset link and emails it. The caller always gets the same answer, whether or not the
     * email is registered, so the endpoint cannot be used to find out who has an account.
     */
    @Transactional
    public void requestPasswordReset(String rawEmail) {
        String email = rawEmail.trim().toLowerCase();
        User user = users.findByEmail(email)
                .filter(u -> u.getStatus() == UserStatus.ACTIVE)
                .orElse(null);
        if (user == null) {
            return;
        }

        LocalDateTime now = LocalDateTime.now(MANILA);
        resetTokens.deleteByExpiresAtBefore(now);

        // Throttle: do not issue a new link if one was sent within the last minute.
        boolean recentlySent = resetTokens.findFirstByUserAndUsedAtIsNullOrderByCreatedAtDesc(user)
                .map(t -> t.getCreatedAt().isAfter(now.minusMinutes(1)))
                .orElse(false);
        if (recentlySent) {
            return;
        }

        resetTokens.deleteByUser(user); // only the newest link works
        String rawToken = newRawToken();
        resetTokens.save(new PasswordResetToken(user, sha256(rawToken), now, now.plusMinutes(expirationMinutes)));
        mailService.sendPasswordReset(user, rawToken);
    }

    // ------------------------------------------------------------------ reset password

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (!request.newPassword().equals(request.confirmNewPassword())) {
            throw fieldError("confirmNewPassword", "Passwords do not match");
        }

        LocalDateTime now = LocalDateTime.now(MANILA);
        PasswordResetToken token = resetTokens.findByTokenHash(sha256(request.token().trim()))
                .orElseThrow(PasswordService::invalidLink);
        Long userId = token.getUser().getId();

        // One atomic update: succeeds only if the token is unused and not expired.
        if (resetTokens.markUsed(token.getId(), now) == 0) {
            throw invalidLink();
        }

        User user = users.findById(userId)
                .orElseThrow(PasswordService::invalidLink);
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setMustChangePassword(false);
        user.incrementTokenVersion();   // ends every existing session
        user.resetFailedAttempts();     // also clears a login lock
        users.save(user);
    }

    // ------------------------------------------------------------------ helpers

    private static ApiException invalidLink() {
        return new ApiException(HttpStatus.BAD_REQUEST,
                "This reset link is invalid or has expired. Please request a new one.");
    }

    private static ApiException fieldError(String field, String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, FIELDS_MESSAGE, Map.of(field, message));
    }

    private static String newRawToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
