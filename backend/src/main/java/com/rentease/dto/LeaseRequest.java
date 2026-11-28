package com.rentease.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LeaseRequest(
        @NotNull(message = "Select a room")
        Long roomId,

        @NotNull(message = "Select a tenant")
        Long tenantId,

        @NotNull(message = "Start date is required")
        LocalDate startDate,

        @NotNull(message = "End date is required")
        LocalDate endDate,

        @NotNull(message = "Monthly rent is required")
        @DecimalMin(value = "0.01", message = "Monthly rent must be greater than 0")
        @Digits(integer = 8, fraction = 2, message = "Monthly rent can have at most 2 decimal places")
        BigDecimal monthlyRent,

        @NotNull(message = "Due day is required")
        @Min(value = 1, message = "Due day must be from 1 to 28")
        @Max(value = 28, message = "Due day must be from 1 to 28")
        Integer dueDay,

        @NotNull(message = "Security deposit is required (enter 0 if none)")
        @DecimalMin(value = "0.00", message = "Security deposit cannot be negative")
        @Digits(integer = 8, fraction = 2, message = "Security deposit can have at most 2 decimal places")
        BigDecimal depositAmount) {
}
