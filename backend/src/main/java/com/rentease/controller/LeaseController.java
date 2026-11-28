package com.rentease.controller;

import com.rentease.dto.LeaseRequest;
import com.rentease.dto.LeaseResponse;
import com.rentease.dto.LeaseUpdateRequest;
import com.rentease.entity.User;
import com.rentease.service.LeaseService;
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

@RestController
@RequestMapping("/api/owner/leases")
public class LeaseController {

    private final LeaseService leaseService;

    public LeaseController(LeaseService leaseService) {
        this.leaseService = leaseService;
    }

    @GetMapping
    public List<LeaseResponse> list(@AuthenticationPrincipal User owner,
                                    @RequestParam(name = "status", required = false) String status) {
        return leaseService.list(owner.getId(), status);
    }

    @GetMapping("/{id}")
    public LeaseResponse get(@AuthenticationPrincipal User owner, @PathVariable("id") Long id) {
        return leaseService.get(owner.getId(), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LeaseResponse create(@AuthenticationPrincipal User owner, @Valid @RequestBody LeaseRequest request) {
        return leaseService.create(owner.getId(), request);
    }

    @PutMapping("/{id}")
    public LeaseResponse update(@AuthenticationPrincipal User owner,
                                @PathVariable("id") Long id,
                                @Valid @RequestBody LeaseUpdateRequest request) {
        return leaseService.update(owner.getId(), id, request);
    }

    @PostMapping("/{id}/end")
    public LeaseResponse end(@AuthenticationPrincipal User owner, @PathVariable("id") Long id) {
        return leaseService.end(owner.getId(), id);
    }
}
