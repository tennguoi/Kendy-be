package com.example.KendyDigital.service.user_management;

import com.example.KendyDigital.dto.role.request.CreateAdminRoleRequest;
import com.example.KendyDigital.dto.role.request.UpdateAdminRoleRequest;
import com.example.KendyDigital.dto.role.response.AdminPermissionResponse;
import com.example.KendyDigital.dto.role.response.AdminRoleResponse;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface AdminRoleService {
    List<AdminRoleResponse> listRoles(Integer limit);
    AdminRoleResponse getRole(UUID id);
    AdminRoleResponse createRole(Long adminUserId, CreateAdminRoleRequest request);
    AdminRoleResponse updateRole(Long adminUserId, UUID id, UpdateAdminRoleRequest request);
    void deleteRole(Long adminUserId, UUID id);
    List<AdminPermissionResponse> listPermissions();
    Map<String, Object> getUserRoles(UUID userId);
    Map<String, Object> setUserRoles(Long adminUserId, UUID userId, List<UUID> roleIds);
    List<String> getUserEffectivePermissions(Long userId);
}
