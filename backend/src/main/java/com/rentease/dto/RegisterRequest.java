package com.rentease.dto;

import com.rentease.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Full name is required")
        @Size(max = 100, message = "Full name must be at most 100 characters")
        String fullName,

        @NotBlank(message = "Email is required")
        @Email(message = "Enter a valid email address")
        @Size(max = 100, message = "Email must be at most 100 characters")
        String email,

        @NotBlank(message = "Mobile number is required")
        @Pattern(regexp = "^(09\\d{9}|\\+639\\d{9})$",
                message = "Enter a valid Philippine mobile number (09XXXXXXXXX or +639XXXXXXXXX)")
        String mobileNumber,

        @NotBlank(message = "Password is required")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$",
                message = "Password must be at least 8 characters and contain a letter and a number")
        String password,

        @NotBlank(message = "Please confirm your password")
        String confirmPassword,

        @NotNull(message = "Select a role (Owner or Tenant)")
        Role role) {
}
