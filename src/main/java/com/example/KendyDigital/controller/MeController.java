package com.example.KendyDigital.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.KendyDigital.dto.AuthUserResponse;
import com.example.KendyDigital.dto.ChangePasswordRequest;
import com.example.KendyDigital.dto.UpdateProfileRequest;
import com.example.KendyDigital.dto.UserDashboardResponse;
import com.example.KendyDigital.security.CurrentUser;
import com.example.KendyDigital.service.UserProfileService;

import jakarta.validation.Valid;

@RestController
public class MeController {
    private final UserProfileService userProfileService;

    public MeController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping("/api/me")
    public AuthUserResponse me(Authentication authentication) {
        return userProfileService.getProfile(CurrentUser.require(authentication).userId());
    }

    @PutMapping("/api/me")
    public AuthUserResponse updateProfile(Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request) {
        return userProfileService.updateProfile(CurrentUser.require(authentication).userId(), request);
    }

    @PostMapping("/api/me/change-password")
    public AuthUserResponse changePassword(Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request) {
        return userProfileService.changePassword(CurrentUser.require(authentication).userId(), request);
    }

    @GetMapping("/api/me/dashboard")
    public UserDashboardResponse dashboard(Authentication authentication) {
        return userProfileService.dashboard(CurrentUser.require(authentication).userId());
    }
}
