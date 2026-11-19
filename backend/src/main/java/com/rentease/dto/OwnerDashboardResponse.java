package com.rentease.dto;

public record OwnerDashboardResponse(
        String fullName,
        String email,
        long registeredTenantAccounts) {
}
