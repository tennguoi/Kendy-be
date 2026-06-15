package com.example.KendyDigital.service.user;

import com.example.KendyDigital.dto.auth.response.AuthUserResponse;
import com.example.KendyDigital.dto.user.request.ChangePasswordRequest;
import com.example.KendyDigital.dto.user.request.UpdateProfileRequest;
import com.example.KendyDigital.dto.user.response.UserDashboardResponse;

public interface UserProfileService {
    AuthUserResponse getProfile(Long userId);
    AuthUserResponse updateProfile(Long userId, UpdateProfileRequest request);
    AuthUserResponse changePassword(Long userId, ChangePasswordRequest request);
    UserDashboardResponse dashboard(Long userId);
}
