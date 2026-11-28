package com.rentease.controller;

import com.rentease.dto.RoomRequest;
import com.rentease.dto.RoomResponse;
import com.rentease.entity.User;
import com.rentease.service.RoomService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** /api/owner/** is limited to the OWNER role in SecurityConfig. */
@RestController
@RequestMapping("/api/owner/rooms")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @GetMapping
    public List<RoomResponse> list(@AuthenticationPrincipal User owner,
                                   @RequestParam(name = "status", required = false) String status,
                                   @RequestParam(name = "search", required = false) String search) {
        return roomService.list(owner.getId(), status, search);
    }

    @GetMapping("/{id}")
    public RoomResponse get(@AuthenticationPrincipal User owner, @PathVariable("id") Long id) {
        return roomService.get(owner.getId(), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RoomResponse create(@AuthenticationPrincipal User owner, @Valid @RequestBody RoomRequest request) {
        return roomService.create(owner.getId(), request);
    }

    @PutMapping("/{id}")
    public RoomResponse update(@AuthenticationPrincipal User owner,
                               @PathVariable("id") Long id,
                               @Valid @RequestBody RoomRequest request) {
        return roomService.update(owner.getId(), id, request);
    }

    @PostMapping("/{id}/archive")
    public RoomResponse archive(@AuthenticationPrincipal User owner, @PathVariable("id") Long id) {
        return roomService.archive(owner.getId(), id);
    }
}
