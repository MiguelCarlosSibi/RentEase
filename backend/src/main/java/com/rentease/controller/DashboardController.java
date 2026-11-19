package com.rentease.controller;

import com.rentease.dto.OwnerDashboardResponse;
import com.rentease.dto.TenantDashboardResponse;
import com.rentease.entity.Role;
import com.rentease.entity.User;
import com.rentease.repository.UserRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Role-protected endpoints: /api/owner/** needs OWNER, /api/tenant/** needs TENANT (see SecurityConfig). */
@RestController
public class DashboardController {

    private final UserRepository users;

    public DashboardController(UserRepository users) {
        this.users = users;
    }

    @GetMapping("/api/owner/dashboard")
    public OwnerDashboardResponse ownerDashboard(@AuthenticationPrincipal User user) {
        return new OwnerDashboardResponse(
                user.getFullName(),
                user.getEmail(),
                users.countByRole(Role.TENANT));
    }

    @GetMapping("/api/tenant/dashboard")
    public TenantDashboardResponse tenantDashboard(@AuthenticationPrincipal User user) {
        return new TenantDashboardResponse(
                user.getFullName(),
                user.getEmail(),
                user.getMobileNumber(),
                user.getCreatedAt());
    }
}
