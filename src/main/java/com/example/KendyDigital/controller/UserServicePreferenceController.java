package com.example.KendyDigital.controller;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.KendyDigital.dto.ServiceResponse;
import com.example.KendyDigital.security.CurrentUser;
import com.example.KendyDigital.service.UserServicePreferenceService;

@RestController
public class UserServicePreferenceController {
    private final UserServicePreferenceService userServicePreferenceService;

    public UserServicePreferenceController(UserServicePreferenceService userServicePreferenceService) {
        this.userServicePreferenceService = userServicePreferenceService;
    }

    @GetMapping("/api/me/favorite-services")
    public List<ServiceResponse> favorites(Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return userServicePreferenceService.favorites(CurrentUser.require(authentication).userId(), page, size);
    }

    @PostMapping("/api/me/favorite-services/{serviceId}")
    public ServiceResponse addFavorite(Authentication authentication, @PathVariable Long serviceId) {
        return userServicePreferenceService.addFavorite(CurrentUser.require(authentication).userId(), serviceId);
    }

    @DeleteMapping("/api/me/favorite-services/{serviceId}")
    public void removeFavorite(Authentication authentication, @PathVariable Long serviceId) {
        userServicePreferenceService.removeFavorite(CurrentUser.require(authentication).userId(), serviceId);
    }

    @GetMapping("/api/me/recent-services")
    public List<ServiceResponse> recent(Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return userServicePreferenceService.recent(CurrentUser.require(authentication).userId(), page, size);
    }
}
