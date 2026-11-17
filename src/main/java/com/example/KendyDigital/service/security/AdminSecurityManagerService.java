package com.example.KendyDigital.service.security;

import com.example.KendyDigital.dto.auth.request.TwoFactorDisableRequest;
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
import java.util.UUID;

public interface AdminSecurityManagerService {
    List<AuthSessionResponse> listSessions(UUID userId, int page, int size);
    List<AuthSessionResponse> listSessions(UUID userId, Integer limit);
    void revokeSession(Long adminUserId, UUID userId, UUID sessionId);
    List<AdminUserResponse> bulkUserStatus(Long adminUserId, List<UUID> ids, UserStatus status, String reason);
    List<AdminUserResponse> listAdmins(Integer limit);
    TotpSetupResponse setupTwoFactor(Long adminUserId, UUID targetAdminId);
    AdminUserResponse verifyAndEnableTwoFactor(Long adminUserId, UUID targetAdminId, TwoFactorVerifyRequest request);
    AdminUserResponse disableTwoFactor(Long adminUserId, UUID targetAdminId, TwoFactorDisableRequest request);
    TotpSetupResponse resetTwoFactor(Long adminUserId, UUID targetAdminId);
    Map<String, Object> getPermissions(UUID adminId);
    Map<String, Object> updatePermissions(Long adminUserId, UUID targetAdminId, AdminPermissionsRequest request);
    Map<String, Object> getRoles(UUID adminId);
    Map<String, Object> updateRoles(Long adminUserId, UUID targetAdminId, AdminRolesRequest request);

    List<UserApiKeyResponse> listUserApiKeys(UUID userId, int page, int size);

    void revokeUserApiKey(Long adminUserId, UUID userId, UUID keyId);
}
