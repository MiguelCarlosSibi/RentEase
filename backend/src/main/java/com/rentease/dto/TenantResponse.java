package com.rentease.dto;

import com.rentease.entity.Tenant;
import com.rentease.entity.UserStatus;

import java.time.LocalDateTime;

public record TenantResponse(
        Long id,
        String fullName,
        String email,
        String mobileNumber,
        String emergencyContactName,
        String emergencyContactNumber,
        boolean active,
        long activeLeases,
        LocalDateTime createdAt) {

    public static TenantResponse from(Tenant tenant, long activeLeases) {
        return new TenantResponse(
                tenant.getId(),
                tenant.getUser().getFullName(),
                tenant.getUser().getEmail(),
                tenant.getUser().getMobileNumber(),
                tenant.getEmergencyContactName(),
                tenant.getEmergencyContactNumber(),
                tenant.getUser().getStatus() == UserStatus.ACTIVE,
                activeLeases,
                tenant.getCreatedAt());
    }
}
