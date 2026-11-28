package com.rentease.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record RoomRequest(
        @NotBlank(message = "Room number is required")
        @Size(max = 20, message = "Room number must be at most 20 characters")
        String roomNumber,

        @Size(max = 255, message = "Description must be at most 255 characters")
        String description,

        @NotNull(message = "Capacity is required")
        @Min(value = 1, message = "Capacity must be at least 1")
        @Max(value = 20, message = "Capacity must be at most 20")
        Integer capacity,

        @NotNull(message = "Monthly rate is required")
        @DecimalMin(value = "0.01", message = "Monthly rate must be greater than 0")
        @Digits(integer = 8, fraction = 2, message = "Monthly rate can have at most 2 decimal places")
        BigDecimal monthlyRate,

        /** Used when editing: true puts the room under maintenance, false makes it available again. */
        Boolean underMaintenance) {
}
