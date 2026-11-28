package com.rentease.service;

import com.rentease.dto.LeaseResponse;
import com.rentease.dto.OwnerDashboardResponse;
import com.rentease.dto.TenantDashboardResponse;
import com.rentease.entity.LeaseStatus;
import com.rentease.entity.RoomStatus;
import com.rentease.entity.User;
import com.rentease.repository.LeaseRepository;
import com.rentease.repository.RoomRepository;
import com.rentease.repository.TenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
public class DashboardService {

    private static final ZoneId MANILA = ZoneId.of("Asia/Manila");

    private final RoomRepository rooms;
    private final TenantRepository tenants;
    private final LeaseRepository leases;

    public DashboardService(RoomRepository rooms, TenantRepository tenants, LeaseRepository leases) {
        this.rooms = rooms;
        this.tenants = tenants;
        this.leases = leases;
    }

    @Transactional(readOnly = true)
    public OwnerDashboardResponse owner(User owner) {
        Long id = owner.getId();
        LocalDate today = LocalDate.now(MANILA);
        // FR020: leases that end within the next 30 days.
        List<LeaseResponse> endingSoon = leases
                .findEndingBetween(id, LeaseStatus.ACTIVE, today, today.plusDays(30)).stream()
                .map(LeaseResponse::from)
                .toList();
        return new OwnerDashboardResponse(
                owner.getFullName(),
                owner.getEmail(),
                rooms.countNotInStatus(id, RoomStatus.ARCHIVED),
                rooms.countInStatus(id, RoomStatus.AVAILABLE),
                tenants.countForOwner(id),
                leases.countForOwner(id, LeaseStatus.ACTIVE),
                endingSoon);
    }

    @Transactional(readOnly = true)
    public TenantDashboardResponse tenant(User user) {
        List<LeaseResponse> mine = leases.findForTenantUser(user.getId(), LeaseStatus.ACTIVE).stream()
                .map(LeaseResponse::from)
                .toList();
        return new TenantDashboardResponse(
                user.getFullName(),
                user.getEmail(),
                user.getMobileNumber(),
                user.getCreatedAt(),
                mine);
    }
}
