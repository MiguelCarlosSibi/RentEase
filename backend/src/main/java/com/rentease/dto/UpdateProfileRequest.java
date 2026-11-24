package com.rentease.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank(message = "Full name is required")
        @Size(max = 100, message = "Full name must be at most 100 characters")
        String fullName,

        @NotBlank(message = "Mobile number is required")
        @Pattern(regexp = "^(09\\d{9}|\\+639\\d{9})$",
                message = "Enter a valid Philippine mobile number (09XXXXXXXXX or +639XXXXXXXXX)")
        String mobileNumber) {
}
