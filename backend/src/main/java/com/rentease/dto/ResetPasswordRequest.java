package com.rentease.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ResetPasswordRequest(
        @NotBlank(message = "The reset link is incomplete. Request a new one.")
        String token,

        @NotBlank(message = "New password is required")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$",
                message = "Password must be at least 8 characters and contain a letter and a number")
        String newPassword,

        @NotBlank(message = "Please confirm your new password")
        String confirmNewPassword) {
}
