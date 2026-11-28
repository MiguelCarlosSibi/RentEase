package com.rentease.controller;

import com.rentease.dto.OwnerDashboardResponse;
import com.rentease.dto.TenantDashboardResponse;
import com.rentease.entity.User;
import com.rentease.service.DashboardService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Role-protected endpoints: /api/owner/** needs OWNER, /api/tenant/** needs TENANT (see SecurityConfig). */
@RestController
public class DashboardController {

    private final DashboardService dashboard;

    public DashboardController(DashboardService dashboard) {
        this.dashboard = dashboard;
    }

    @GetMapping("/api/owner/dashboard")
    public OwnerDashboardResponse ownerDashboard(@AuthenticationPrincipal User user) {
        return dashboard.owner(user);
    }

    @GetMapping("/api/tenant/dashboard")
    public TenantDashboardResponse tenantDashboard(@AuthenticationPrincipal User user) {
        return dashboard.tenant(user);
    }
}
