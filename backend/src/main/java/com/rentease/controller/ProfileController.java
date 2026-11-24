package com.rentease.controller;

import com.rentease.dto.UpdateProfileRequest;
import com.rentease.dto.UserResponse;
import com.rentease.entity.User;
import com.rentease.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The profile endpoints have no user id in the URL: they always act on the logged-in user,
 * so nobody can read or change another user's profile through them.
 */
@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public UserResponse getProfile(@AuthenticationPrincipal User user) {
        return profileService.getProfile(user.getId());
    }

    @PutMapping
    public UserResponse updateProfile(@AuthenticationPrincipal User user,
                                      @Valid @RequestBody UpdateProfileRequest request) {
        return profileService.updateProfile(user.getId(), request);
    }
}
