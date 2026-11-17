package com.example.KendyDigital.controller;

import com.example.KendyDigital.dto.role.request.CreateAdminRoleRequest;
import com.example.KendyDigital.dto.role.request.UpdateAdminRoleRequest;
import com.example.KendyDigital.dto.role.response.AdminPermissionResponse;
import com.example.KendyDigital.dto.role.response.AdminRoleResponse;
import com.example.KendyDigital.security.CurrentUser;
import com.example.KendyDigital.service.user_management.AdminRoleService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminRoleController {
    private final AdminRoleService adminRoleService;

    public AdminRoleController(AdminRoleService adminRoleService) {
        this.adminRoleService = adminRoleService;
    }

    @GetMapping("/api/admin/roles")
    public List<AdminRoleResponse> listRoles(@RequestParam(required = false) Integer limit) {
        return adminRoleService.listRoles(limit);
    }

    @GetMapping("/api/admin/roles/{id}")
    public AdminRoleResponse getRole(@PathVariable UUID id) {
        return adminRoleService.getRole(id);
    }

    @PostMapping("/api/admin/roles")
    public AdminRoleResponse createRole(Authentication authentication,
            @Valid @RequestBody CreateAdminRoleRequest request) {
        return adminRoleService.createRole(CurrentUser.require(authentication).userId(), request);
    }

    @PutMapping("/api/admin/roles/{id}")
    public AdminRoleResponse updateRole(Authentication authentication, @PathVariable UUID id,
            @Valid @RequestBody UpdateAdminRoleRequest request) {
        return adminRoleService.updateRole(CurrentUser.require(authentication).userId(), id, request);
    }

    @DeleteMapping("/api/admin/roles/{id}")
    public void deleteRole(Authentication authentication, @PathVariable UUID id) {
        adminRoleService.deleteRole(CurrentUser.require(authentication).userId(), id);
    }

    @GetMapping("/api/admin/permissions")
    public List<AdminPermissionResponse> listPermissions() {
        return adminRoleService.listPermissions();
    }

    @GetMapping("/api/admin/users/{userId}/roles")
    public Map<String, Object> getUserRoles(@PathVariable UUID userId) {
        return adminRoleService.getUserRoles(userId);
    }

    @PutMapping("/api/admin/users/{userId}/roles")
    public Map<String, Object> setUserRoles(Authentication authentication, @PathVariable UUID userId,
            @RequestBody List<UUID> roleIds) {
        return adminRoleService.setUserRoles(CurrentUser.require(authentication).userId(), userId, roleIds);
    }
}
