package com.rentease.dto;

import java.time.LocalDateTime;
import java.util.List;

public record TenantDashboardResponse(
        String fullName,
        String email,
        String mobileNumber,
        LocalDateTime memberSince,
        List<LeaseResponse> leases) {
}
