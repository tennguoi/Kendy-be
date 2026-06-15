package com.example.KendyDigital.controller;

import com.example.KendyDigital.dto.auth.request.TwoFactorVerifyRequest;
import com.example.KendyDigital.dto.auth.response.AuthSessionResponse;
import com.example.KendyDigital.dto.auth.response.TotpSetupResponse;
import com.example.KendyDigital.dto.catalog.request.IdsRequest;
import com.example.KendyDigital.dto.role.request.AdminPermissionsRequest;
import com.example.KendyDigital.dto.role.request.AdminRolesRequest;
import com.example.KendyDigital.dto.user.response.AdminUserResponse;
import com.example.KendyDigital.model.user.UserStatus;
import com.example.KendyDigital.security.CurrentUser;
import com.example.KendyDigital.service.security.AdminSecurityManagerService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminSecurityController {
    private final AdminSecurityManagerService securityManagerService;

    public AdminSecurityController(AdminSecurityManagerService securityManagerService) {
        this.securityManagerService = securityManagerService;
    }

    @GetMapping("/api/admin/users/{id}/sessions")
    public List<AuthSessionResponse> userSessions(@PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return securityManagerService.listSessions(id, page, size);
    }

    @DeleteMapping("/api/admin/users/{id}/sessions/{sessionId}")
    public void revokeUserSession(Authentication authentication, @PathVariable Long id,
            @PathVariable Long sessionId) {
        securityManagerService.revokeSession(CurrentUser.require(authentication).userId(), id, sessionId);
    }

    @PostMapping("/api/admin/users/bulk-lock")
    public List<AdminUserResponse> bulkLockUsers(Authentication authentication,
            @Valid @RequestBody IdsRequest request) {
        return securityManagerService.bulkUserStatus(CurrentUser.require(authentication).userId(), request.ids(),
                UserStatus.LOCKED, request.reason());
    }

    @PostMapping("/api/admin/users/bulk-unlock")
    public List<AdminUserResponse> bulkUnlockUsers(Authentication authentication,
            @Valid @RequestBody IdsRequest request) {
        return securityManagerService.bulkUserStatus(CurrentUser.require(authentication).userId(), request.ids(),
                UserStatus.ACTIVE, request.reason());
    }

    @GetMapping("/api/admin/admins")
    public List<AdminUserResponse> admins(@RequestParam(required = false) Integer limit) {
        return securityManagerService.listAdmins(limit);
    }

    @PostMapping("/api/admin/admins/{id}/2fa/setup")
    public TotpSetupResponse setupTwoFactor(Authentication authentication, @PathVariable Long id) {
        return securityManagerService.setupTwoFactor(CurrentUser.require(authentication).userId(), id);
    }

    @PostMapping("/api/admin/admins/{id}/2fa/enable")
    public AdminUserResponse enableTwoFactor(Authentication authentication, @PathVariable Long id,
            @Valid @RequestBody TwoFactorVerifyRequest request) {
        return securityManagerService.verifyAndEnableTwoFactor(CurrentUser.require(authentication).userId(), id, request);
    }

    @PostMapping("/api/admin/admins/{id}/2fa/disable")
    public AdminUserResponse disableTwoFactor(Authentication authentication, @PathVariable Long id) {
        return securityManagerService.disableTwoFactor(CurrentUser.require(authentication).userId(), id);
    }

    @PostMapping("/api/admin/admins/{id}/2fa/reset")
    public TotpSetupResponse resetTwoFactor(Authentication authentication, @PathVariable Long id) {
        return securityManagerService.resetTwoFactor(CurrentUser.require(authentication).userId(), id);
    }

    @GetMapping("/api/admin/admins/{id}/permissions")
    public Map<String, Object> getPermissions(@PathVariable Long id) {
        return securityManagerService.getPermissions(id);
    }

    @PutMapping("/api/admin/admins/{id}/permissions")
    public Map<String, Object> updatePermissions(Authentication authentication, @PathVariable Long id,
            @RequestBody AdminPermissionsRequest request) {
        return securityManagerService.updatePermissions(CurrentUser.require(authentication).userId(), id, request);
    }

    @GetMapping("/api/admin/admins/{id}/roles")
    public Map<String, Object> getRoles(@PathVariable Long id) {
        return securityManagerService.getRoles(id);
    }

    @PutMapping("/api/admin/admins/{id}/roles")
    public Map<String, Object> updateRoles(Authentication authentication, @PathVariable Long id,
            @Valid @RequestBody AdminRolesRequest request) {
        return securityManagerService.updateRoles(CurrentUser.require(authentication).userId(), id, request);
    }

    @GetMapping("/api/admin/admins/{id}/sessions")
    public List<AuthSessionResponse> adminSessions(@PathVariable Long id,
            @RequestParam(required = false) Integer limit) {
        return securityManagerService.listSessions(id, limit);
    }

    @DeleteMapping("/api/admin/admins/{id}/sessions/{sessionId}")
    public void revokeAdminSession(Authentication authentication, @PathVariable Long id,
            @PathVariable Long sessionId) {
        securityManagerService.revokeSession(CurrentUser.require(authentication).userId(), id, sessionId);
    }

    @PostMapping("/api/admin/admins/bulk-lock")
    public List<AdminUserResponse> bulkLockAdmins(Authentication authentication,
            @Valid @RequestBody IdsRequest request) {
        return securityManagerService.bulkUserStatus(CurrentUser.require(authentication).userId(), request.ids(),
                UserStatus.LOCKED, request.reason());
    }

    @PostMapping("/api/admin/admins/bulk-unlock")
    public List<AdminUserResponse> bulkUnlockAdmins(Authentication authentication,
            @Valid @RequestBody IdsRequest request) {
        return securityManagerService.bulkUserStatus(CurrentUser.require(authentication).userId(), request.ids(),
                UserStatus.ACTIVE, request.reason());
    }
}
