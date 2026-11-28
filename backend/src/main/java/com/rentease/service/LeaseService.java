package com.rentease.service;

import com.rentease.dto.LeaseRequest;
import com.rentease.dto.LeaseResponse;
import com.rentease.dto.LeaseUpdateRequest;
import com.rentease.entity.Lease;
import com.rentease.entity.LeaseStatus;
import com.rentease.entity.Room;
import com.rentease.entity.RoomStatus;
import com.rentease.entity.Tenant;
import com.rentease.entity.UserStatus;
import com.rentease.exception.ApiException;
import com.rentease.repository.LeaseRepository;
import com.rentease.repository.RoomRepository;
import com.rentease.repository.TenantRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Lease management for owners (FR016 to FR019). */
@Service
public class LeaseService {

    private static final ZoneId MANILA = ZoneId.of("Asia/Manila");
    private static final String FIELDS_MESSAGE = "Please correct the highlighted fields";

    private final LeaseRepository leases;
    private final RoomRepository rooms;
    private final TenantRepository tenants;

    public LeaseService(LeaseRepository leases, RoomRepository rooms, TenantRepository tenants) {
        this.leases = leases;
        this.rooms = rooms;
        this.tenants = tenants;
    }

    @Transactional(readOnly = true)
    public List<LeaseResponse> list(Long ownerId, String status) {
        List<Lease> found;
        if (status == null || status.isBlank() || status.equalsIgnoreCase("ALL")) {
            found = leases.findAllForOwner(ownerId);
        } else {
            try {
                found = leases.findForOwnerByStatus(ownerId, LeaseStatus.valueOf(status.trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ex) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Unknown lease status: " + status);
            }
        }
        return found.stream().map(LeaseResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public LeaseResponse get(Long ownerId, Long id) {
        return LeaseResponse.from(find(ownerId, id));
    }

    /** FR016 and FR017: the room must have a free place, so a lease can never exceed the room capacity. */
    @Transactional
    public LeaseResponse create(Long ownerId, LeaseRequest request) {
        // The room row is locked until this transaction ends, so two requests cannot both take the last place.
        Room room = rooms.findForUpdate(request.roomId(), ownerId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Room not found"));
        Tenant tenant = tenants.findOneForOwner(request.tenantId(), ownerId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Tenant not found"));

        LocalDate today = LocalDate.now(MANILA);
        Map<String, String> errors = new LinkedHashMap<>();
        if (!request.endDate().isAfter(request.startDate())) {
            errors.put("endDate", "End date must be after the start date");
        } else if (request.endDate().isBefore(today)) {
            errors.put("endDate", "End date cannot be in the past");
        }
        if (!errors.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, FIELDS_MESSAGE, errors);
        }

        if (room.getStatus() == RoomStatus.ARCHIVED || room.getStatus() == RoomStatus.MAINTENANCE) {
            throw new ApiException(HttpStatus.CONFLICT, "Room " + room.getRoomNumber()
                    + " is not open for new leases (status: " + room.getStatus() + ").");
        }
        if (tenant.getUser().getStatus() != UserStatus.ACTIVE) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "This tenant's account is inactive. Activate the tenant before creating a lease.");
        }
        if (leases.existsForRoomAndTenant(room.getId(), tenant.getId(), LeaseStatus.ACTIVE)) {
            throw new ApiException(HttpStatus.CONFLICT, "This tenant already has an active lease in room "
                    + room.getRoomNumber() + ".");
        }
        long active = leases.countForRoom(room.getId(), LeaseStatus.ACTIVE);
        if (active >= room.getCapacity()) {
            throw new ApiException(HttpStatus.CONFLICT, "Room " + room.getRoomNumber() + " is full ("
                    + active + " of " + room.getCapacity() + " places taken).");
        }

        Lease lease = new Lease();
        lease.setRoom(room);
        lease.setTenant(tenant);
        lease.setStartDate(request.startDate());
        lease.setEndDate(request.endDate());
        lease.setMonthlyRent(request.monthlyRent().setScale(2, RoundingMode.HALF_UP));
        lease.setDueDay(request.dueDay());
        lease.setDepositAmount(request.depositAmount().setScale(2, RoundingMode.HALF_UP));
        lease.setStatus(LeaseStatus.ACTIVE);
        lease.touch(ownerId);
        leases.save(lease);

        room.refreshOccupancy(active + 1);
        rooms.save(room);
        return LeaseResponse.from(lease);
    }

    /** FR018: only the rent and the due day can change, and only on a lease that is still active. */
    @Transactional
    public LeaseResponse update(Long ownerId, Long id, LeaseUpdateRequest request) {
        Lease lease = find(ownerId, id);
        if (lease.getStatus() != LeaseStatus.ACTIVE) {
            throw new ApiException(HttpStatus.CONFLICT, "An ended lease cannot be edited.");
        }
        lease.setMonthlyRent(request.monthlyRent().setScale(2, RoundingMode.HALF_UP));
        lease.setDueDay(request.dueDay());
        lease.touch(ownerId);
        return LeaseResponse.from(leases.save(lease));
    }

    /** FR019: ending a lease stops further billing and frees the place in the room. */
    @Transactional
    public LeaseResponse end(Long ownerId, Long id) {
        Lease lease = find(ownerId, id);
        if (lease.getStatus() != LeaseStatus.ACTIVE) {
            throw new ApiException(HttpStatus.CONFLICT, "This lease has already ended.");
        }
        Room room = rooms.findForUpdate(lease.getRoom().getId(), ownerId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Room not found"));
        long activeBefore = leases.countForRoom(room.getId(), LeaseStatus.ACTIVE);

        LocalDate today = LocalDate.now(MANILA);
        lease.setStatus(LeaseStatus.ENDED);
        if (lease.getEndDate().isAfter(today)) {
            lease.setEndDate(today.isBefore(lease.getStartDate()) ? lease.getStartDate() : today);
        }
        lease.touch(ownerId);
        leases.save(lease);

        room.refreshOccupancy(activeBefore - 1);
        rooms.save(room);
        return LeaseResponse.from(lease);
    }

    private Lease find(Long ownerId, Long id) {
        return leases.findOneForOwner(id, ownerId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Lease not found"));
    }
}
