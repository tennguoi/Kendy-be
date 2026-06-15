package com.example.KendyDigital.service.user;

import com.example.KendyDigital.dto.user.request.AdminUserRoleUpdateRequest;
import com.example.KendyDigital.dto.user.request.AdminUserStatusUpdateRequest;
import com.example.KendyDigital.dto.user.response.AdminUserDetailResponse;
import com.example.KendyDigital.dto.user.response.AdminUserResponse;
import com.example.KendyDigital.model.user.UserStatus;
import java.util.List;

public interface AdminUserManagerService {
    List<AdminUserResponse> listUsers(UserStatus status);
    List<AdminUserResponse> listUsers(UserStatus status, int page, int size);
    List<AdminUserResponse> listUsers(UserStatus status, Integer limit);
    List<AdminUserResponse> searchUsers(String query, UserStatus status, int page, int size);
    List<AdminUserResponse> searchUsers(String query, UserStatus status, Integer limit);
    AdminUserDetailResponse getUserDetail(Long userId);
    AdminUserResponse updateUserStatus(Long adminUserId, Long targetUserId, AdminUserStatusUpdateRequest request);
    AdminUserResponse updateUserRole(Long adminUserId, Long targetUserId, AdminUserRoleUpdateRequest request);
}
