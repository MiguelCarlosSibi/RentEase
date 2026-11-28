package com.rentease.dto;

import java.util.List;

public record OwnerDashboardResponse(
        String fullName,
        String email,
        long totalRooms,
        long availableRooms,
        long totalTenants,
        long activeLeases,
        List<LeaseResponse> leasesEndingSoon) {
}
