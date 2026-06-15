package com.example.KendyDigital.service.role;

import com.example.KendyDigital.dto.role.request.CreateAdminRoleRequest;
import com.example.KendyDigital.dto.role.request.UpdateAdminRoleRequest;
import com.example.KendyDigital.dto.role.response.AdminPermissionResponse;
import com.example.KendyDigital.dto.role.response.AdminRoleResponse;
import java.util.List;
import java.util.Map;

public interface AdminRoleService {
    List<AdminRoleResponse> listRoles();
    AdminRoleResponse getRole(Long id);
    AdminRoleResponse createRole(Long adminUserId, CreateAdminRoleRequest request);
    AdminRoleResponse updateRole(Long adminUserId, Long id, UpdateAdminRoleRequest request);
    void deleteRole(Long adminUserId, Long id);
    List<AdminPermissionResponse> listPermissions();
    Map<String, Object> getUserRoles(Long userId);
    Map<String, Object> setUserRoles(Long adminUserId, Long userId, List<Long> roleIds);
    List<String> getUserEffectivePermissions(Long userId);
}
