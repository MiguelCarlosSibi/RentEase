package com.rentease.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** The email (login ID) cannot be changed here. */
public record TenantUpdateRequest(
        @NotBlank(message = "Full name is required")
        @Size(max = 100, message = "Full name must be at most 100 characters")
        String fullName,

        @NotBlank(message = "Mobile number is required")
        @Pattern(regexp = "^(09\\d{9}|\\+639\\d{9})$",
                message = "Enter a valid Philippine mobile number (09XXXXXXXXX or +639XXXXXXXXX)")
        String mobileNumber,

        @NotBlank(message = "Emergency contact name is required")
        @Size(max = 100, message = "Emergency contact name must be at most 100 characters")
        String emergencyContactName,

        @NotBlank(message = "Emergency contact number is required")
        @Pattern(regexp = "^(09\\d{9}|\\+639\\d{9})$",
                message = "Enter a valid Philippine mobile number (09XXXXXXXXX or +639XXXXXXXXX)")
        String emergencyContactNumber) {
}
