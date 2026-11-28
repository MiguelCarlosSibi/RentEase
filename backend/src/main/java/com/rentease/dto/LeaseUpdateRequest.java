package com.rentease.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Only the rent and the due day can be changed. They apply to bills that are not yet generated. */
public record LeaseUpdateRequest(
        @NotNull(message = "Monthly rent is required")
        @DecimalMin(value = "0.01", message = "Monthly rent must be greater than 0")
        @Digits(integer = 8, fraction = 2, message = "Monthly rent can have at most 2 decimal places")
        BigDecimal monthlyRent,

        @NotNull(message = "Due day is required")
        @Min(value = 1, message = "Due day must be from 1 to 28")
        @Max(value = 28, message = "Due day must be from 1 to 28")
        Integer dueDay) {
}
