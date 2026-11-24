package com.rentease.service;

import com.rentease.dto.UpdateProfileRequest;
import com.rentease.dto.UserResponse;
import com.rentease.entity.User;
import com.rentease.exception.ApiException;
import com.rentease.repository.UserRepository;
import com.rentease.util.PhoneNumbers;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {

    private final UserRepository users;

    public ProfileService(UserRepository users) {
        this.users = users;
    }

    @Transactional(readOnly = true)
    public UserResponse getProfile(Long userId) {
        return UserResponse.from(load(userId));
    }

    /**
     * The id always comes from the authenticated user, never from the request,
     * so a user can only change their own profile.
     */
    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = load(userId);
        user.setFullName(request.fullName().trim());
        user.setMobileNumber(PhoneNumbers.normalize(request.mobileNumber()));
        return UserResponse.from(users.save(user));
    }

    private User load(Long userId) {
        return users.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
    }
}
