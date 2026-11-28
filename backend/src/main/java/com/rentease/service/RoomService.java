package com.rentease.service;

import com.rentease.dto.RoomRequest;
import com.rentease.dto.RoomResponse;
import com.rentease.entity.Lease;
import com.rentease.entity.LeaseStatus;
import com.rentease.entity.Room;
import com.rentease.entity.RoomStatus;
import com.rentease.entity.User;
import com.rentease.exception.ApiException;
import com.rentease.repository.LeaseRepository;
import com.rentease.repository.RoomRepository;
import com.rentease.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/** Room management for owners (FR010, FR011, FR012). Every query is limited to the signed-in owner. */
@Service
public class RoomService {

    private static final String FIELDS_MESSAGE = "Please correct the highlighted fields";

    private final RoomRepository rooms;
    private final LeaseRepository leases;
    private final UserRepository users;

    public RoomService(RoomRepository rooms, LeaseRepository leases, UserRepository users) {
        this.rooms = rooms;
        this.leases = leases;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<RoomResponse> list(Long ownerId, String status, String search) {
        RoomStatus wanted = parseStatus(status);
        String q = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        Map<Long, Long> occupancy = leases.findForOwnerByStatus(ownerId, LeaseStatus.ACTIVE).stream()
                .collect(Collectors.groupingBy(l -> l.getRoom().getId(), Collectors.counting()));

        return rooms.findAllForOwner(ownerId).stream()
                // Archived rooms are hidden unless they are asked for.
                .filter(r -> wanted == null ? r.getStatus() != RoomStatus.ARCHIVED : r.getStatus() == wanted)
                .filter(r -> q.isEmpty()
                        || r.getRoomNumber().toLowerCase(Locale.ROOT).contains(q)
                        || (r.getDescription() != null && r.getDescription().toLowerCase(Locale.ROOT).contains(q)))
                .map(r -> RoomResponse.from(r, occupancy.getOrDefault(r.getId(), 0L)))
                .toList();
    }

    @Transactional(readOnly = true)
    public RoomResponse get(Long ownerId, Long id) {
        Room room = find(ownerId, id);
        return RoomResponse.from(room, leases.countForRoom(room.getId(), LeaseStatus.ACTIVE));
    }

    @Transactional
    public RoomResponse create(Long ownerId, RoomRequest request) {
        String number = request.roomNumber().trim();
        if (rooms.existsNumber(ownerId, number)) {
            throw duplicateNumber();
        }
        Room room = new Room();
        room.setOwner(users.getReferenceById(ownerId));
        apply(room, request, number);
        room.setStatus(RoomStatus.AVAILABLE);
        if (Boolean.TRUE.equals(request.underMaintenance())) {
            room.setStatus(RoomStatus.MAINTENANCE);
        }
        return RoomResponse.from(rooms.save(room), 0);
    }

    @Transactional
    public RoomResponse update(Long ownerId, Long id, RoomRequest request) {
        Room room = find(ownerId, id);
        if (room.getStatus() == RoomStatus.ARCHIVED) {
            throw new ApiException(HttpStatus.CONFLICT, "An archived room cannot be edited.");
        }
        String number = request.roomNumber().trim();
        if (rooms.existsNumberExcept(ownerId, number, room.getId())) {
            throw duplicateNumber();
        }
        long active = leases.countForRoom(room.getId(), LeaseStatus.ACTIVE);
        if (request.capacity() < active) {
            throw new ApiException(HttpStatus.CONFLICT, FIELDS_MESSAGE, Map.of("capacity",
                    "Capacity cannot be lower than the " + active + " active lease(s) in this room"));
        }
        apply(room, request, number);
        if (Boolean.TRUE.equals(request.underMaintenance())) {
            room.setStatus(RoomStatus.MAINTENANCE);
        } else {
            if (room.getStatus() == RoomStatus.MAINTENANCE) {
                room.setStatus(RoomStatus.AVAILABLE);
            }
            room.refreshOccupancy(active);
        }
        return RoomResponse.from(rooms.save(room), active);
    }

    /** FR012: a room can be archived only when it has no active lease. */
    @Transactional
    public RoomResponse archive(Long ownerId, Long id) {
        Room room = find(ownerId, id);
        long active = leases.countForRoom(room.getId(), LeaseStatus.ACTIVE);
        if (active > 0) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "This room still has " + active + " active lease(s). End them before archiving the room.");
        }
        room.setStatus(RoomStatus.ARCHIVED);
        return RoomResponse.from(rooms.save(room), 0);
    }

    // ------------------------------------------------------------------ helpers

    private Room find(Long ownerId, Long id) {
        return rooms.findOneForOwner(id, ownerId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Room not found"));
    }

    private static void apply(Room room, RoomRequest request, String number) {
        room.setRoomNumber(number);
        String description = request.description() == null ? "" : request.description().trim();
        room.setDescription(description.isEmpty() ? null : description);
        room.setCapacity(request.capacity());
        room.setMonthlyRate(request.monthlyRate().setScale(2, RoundingMode.HALF_UP));
    }

    private static RoomStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return RoomStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unknown room status: " + status);
        }
    }

    private static ApiException duplicateNumber() {
        return new ApiException(HttpStatus.CONFLICT, FIELDS_MESSAGE,
                Map.of("roomNumber", "You already have a room with this number"));
    }
}
