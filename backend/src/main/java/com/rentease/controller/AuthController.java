package com.rentease.controller;

import com.rentease.dto.AuthResponse;
import com.rentease.dto.ChangePasswordRequest;
import com.rentease.dto.ForgotPasswordRequest;
import com.rentease.dto.LoginRequest;
import com.rentease.dto.RegisterRequest;
import com.rentease.dto.ResetPasswordRequest;
import com.rentease.dto.UserResponse;
import com.rentease.entity.User;
import com.rentease.service.AuthService;
import com.rentease.service.PasswordService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final PasswordService passwordService;

    public AuthController(AuthService authService, PasswordService passwordService) {
        this.authService = authService;
        this.passwordService = passwordService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/logout")
    public Map<String, String> logout(@RequestHeader("Authorization") String authorization) {
        if (authorization.startsWith("Bearer ")) {
            authService.logout(authorization.substring(7));
        }
        return Map.of("message", "Logged out");
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal User user) {
        return UserResponse.from(user);
    }

    /** Logged-in users only. Returns a new token because the old one stops working after the change. */
    @PostMapping("/change-password")
    public AuthResponse changePassword(@AuthenticationPrincipal User user,
                                       @Valid @RequestBody ChangePasswordRequest request) {
        return passwordService.changePassword(user.getId(), request);
    }

    /** Public. Always answers the same way so it does not reveal which emails are registered. */
    @PostMapping("/forgot-password")
    public Map<String, String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordService.requestPasswordReset(request.email());
        return Map.of("message", "If that email is registered, a password reset link has been sent.");
    }

    /** Public, but needs a valid, unused, unexpired token from the reset link. */
    @PostMapping("/reset-password")
    public Map<String, String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordService.resetPassword(request);
        return Map.of("message", "Your password has been reset. You can now log in.");
    }
}
