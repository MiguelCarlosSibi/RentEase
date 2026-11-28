package com.rentease.controller;

import com.rentease.dto.TenantCreateResponse;
import com.rentease.dto.TenantRequest;
import com.rentease.dto.TenantResponse;
import com.rentease.dto.TenantUpdateRequest;
import com.rentease.entity.User;
import com.rentease.service.TenantService;
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
@RequestMapping("/api/owner/tenants")
public class TenantController {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @GetMapping
    public List<TenantResponse> list(@AuthenticationPrincipal User owner,
                                     @RequestParam(name = "search", required = false) String search) {
        return tenantService.list(owner.getId(), search);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TenantCreateResponse create(@AuthenticationPrincipal User owner,
                                       @Valid @RequestBody TenantRequest request) {
        return tenantService.create(owner.getId(), request);
    }

    @PutMapping("/{id}")
    public TenantResponse update(@AuthenticationPrincipal User owner,
                                 @PathVariable("id") Long id,
                                 @Valid @RequestBody TenantUpdateRequest request) {
        return tenantService.update(owner.getId(), id, request);
    }

    @PostMapping("/{id}/deactivate")
    public TenantResponse deactivate(@AuthenticationPrincipal User owner, @PathVariable("id") Long id) {
        return tenantService.setActive(owner.getId(), id, false);
    }

    @PostMapping("/{id}/activate")
    public TenantResponse activate(@AuthenticationPrincipal User owner, @PathVariable("id") Long id) {
        return tenantService.setActive(owner.getId(), id, true);
    }
}
