package com.rentease.dto;

import java.time.LocalDateTime;

public record TenantDashboardResponse(
        String fullName,
        String email,
        String mobileNumber,
        LocalDateTime memberSince) {
}
