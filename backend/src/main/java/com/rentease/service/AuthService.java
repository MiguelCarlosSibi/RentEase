package com.rentease.service;

import com.rentease.dto.AuthResponse;
import com.rentease.dto.LoginRequest;
import com.rentease.dto.RegisterRequest;
import com.rentease.dto.UserResponse;
import com.rentease.entity.RevokedToken;
import com.rentease.entity.User;
import com.rentease.entity.UserStatus;
import com.rentease.exception.ApiException;
import com.rentease.repository.RevokedTokenRepository;
import com.rentease.repository.UserRepository;
import com.rentease.security.JwtService;
import com.rentease.util.PhoneNumbers;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

@Service
public class AuthService {

    private static final ZoneId MANILA = ZoneId.of("Asia/Manila");
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_MINUTES = 15;

    private final UserRepository users;
    private final RevokedTokenRepository revokedTokens;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final String dummyHash;

    public AuthService(UserRepository users,
                       RevokedTokenRepository revokedTokens,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.users = users;
        this.revokedTokens = revokedTokens;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        // Used so an unknown email takes about as long as a wrong password.
        this.dummyHash = passwordEncoder.encode("rentease-dummy-password");
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (!request.password().equals(request.confirmPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Please correct the highlighted fields",
                    Map.of("confirmPassword", "Passwords do not match"));
        }

        String email = request.email().trim().toLowerCase();
        if (users.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "Email is already registered",
                    Map.of("email", "This email is already registered"));
        }

        User user = new User();
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setMobileNumber(PhoneNumbers.normalize(request.mobileNumber()));
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(request.role());
        user.setStatus(UserStatus.ACTIVE);
        return UserResponse.from(users.save(user));
    }

    /** noRollbackFor keeps the failed-attempt counter even though an exception is thrown. */
    @Transactional(noRollbackFor = ApiException.class)
    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        User user = users.findByEmail(email).orElse(null);

        if (user == null) {
            passwordEncoder.matches(request.password(), dummyHash);
            throw invalidCredentials();
        }

        LocalDateTime now = LocalDateTime.now(MANILA);
        if (user.isLocked(now)) {
            throw new ApiException(HttpStatus.LOCKED,
                    "Account locked. Try again in a few minutes.");
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This account is inactive.");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            user.recordFailedLogin(now, MAX_FAILED_ATTEMPTS, LOCK_MINUTES);
            users.save(user);
            if (user.isLocked(now)) {
                throw new ApiException(HttpStatus.LOCKED,
                        "Too many failed attempts. Account locked for " + LOCK_MINUTES + " minutes.");
            }
            throw invalidCredentials();
        }

        user.resetFailedAttempts();
        users.save(user);
        return new AuthResponse(jwtService.generateToken(user), UserResponse.from(user));
    }

    /** Ends the session on the server: the token is stored as revoked until it would have expired. */
    @Transactional
    public void logout(String token) {
        try {
            Claims claims = jwtService.parse(token);
            LocalDateTime expiresAt = LocalDateTime.ofInstant(claims.getExpiration().toInstant(), MANILA);
            if (!revokedTokens.existsById(claims.getId())) {
                revokedTokens.save(new RevokedToken(claims.getId(), expiresAt));
            }
            revokedTokens.deleteByExpiresAtBefore(LocalDateTime.now(MANILA));
        } catch (JwtException | IllegalArgumentException ex) {
            // Already invalid or expired: nothing to revoke.
        }
    }

    private ApiException invalidCredentials() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
    }
}
