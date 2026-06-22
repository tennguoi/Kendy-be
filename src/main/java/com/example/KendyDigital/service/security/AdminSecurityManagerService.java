package com.example.KendyDigital.service.security;

import com.example.KendyDigital.dto.auth.request.TwoFactorVerifyRequest;
import com.example.KendyDigital.dto.auth.response.AuthSessionResponse;
import com.example.KendyDigital.dto.auth.response.TotpSetupResponse;
import com.example.KendyDigital.dto.role.request.AdminPermissionsRequest;
import com.example.KendyDigital.dto.role.request.AdminRolesRequest;
import com.example.KendyDigital.dto.user.response.AdminUserResponse;
import com.example.KendyDigital.dto.user.response.UserApiKeyResponse;
import com.example.KendyDigital.model.user.UserStatus;
import java.util.List;
import java.util.Map;

public interface AdminSecurityManagerService {
    List<AuthSessionResponse> listSessions(Long userId, int page, int size);
    List<AuthSessionResponse> listSessions(Long userId, Integer limit);
    void revokeSession(Long adminUserId, Long userId, Long sessionId);
    List<AdminUserResponse> bulkUserStatus(Long adminUserId, List<Long> ids, UserStatus status, String reason);
    List<AdminUserResponse> listAdmins(Integer limit);
    TotpSetupResponse setupTwoFactor(Long adminUserId, Long targetAdminId);
    AdminUserResponse verifyAndEnableTwoFactor(Long adminUserId, Long targetAdminId, TwoFactorVerifyRequest request);
    AdminUserResponse disableTwoFactor(Long adminUserId, Long targetAdminId);
    TotpSetupResponse resetTwoFactor(Long adminUserId, Long targetAdminId);
    Map<String, Object> getPermissions(Long adminId);
    Map<String, Object> updatePermissions(Long adminUserId, Long targetAdminId, AdminPermissionsRequest request);
    Map<String, Object> getRoles(Long adminId);
    Map<String, Object> updateRoles(Long adminUserId, Long targetAdminId, AdminRolesRequest request);

    List<UserApiKeyResponse> listUserApiKeys(Long userId, int page, int size);

    void revokeUserApiKey(Long adminUserId, Long userId, Long keyId);
}
