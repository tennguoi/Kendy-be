package com.example.KendyDigital.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.KendyDigital.dto.role.response.AdminPermissionResponse;
import com.example.KendyDigital.dto.role.response.AdminRoleResponse;
import com.example.KendyDigital.dto.role.request.CreateAdminRoleRequest;
import com.example.KendyDigital.dto.role.request.UpdateAdminRoleRequest;
import com.example.KendyDigital.model.AdminPermission;
import com.example.KendyDigital.model.AdminRole;
import com.example.KendyDigital.model.RolePermission;
import com.example.KendyDigital.model.UserAccount;
import com.example.KendyDigital.model.UserAdminRole;
import com.example.KendyDigital.repository.AdminPermissionRepository;
import com.example.KendyDigital.repository.AdminRoleRepository;
import com.example.KendyDigital.repository.RolePermissionRepository;
import com.example.KendyDigital.repository.UserAccountRepository;
import com.example.KendyDigital.repository.UserAdminRoleRepository;

@Service
public class AdminRoleService {
    private final AdminRoleRepository adminRoleRepository;
    private final AdminPermissionRepository adminPermissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserAdminRoleRepository userAdminRoleRepository;
    private final UserAccountRepository userAccountRepository;
    private final AuditService auditService;

    public AdminRoleService(AdminRoleRepository adminRoleRepository,
            AdminPermissionRepository adminPermissionRepository,
            RolePermissionRepository rolePermissionRepository,
            UserAdminRoleRepository userAdminRoleRepository,
            UserAccountRepository userAccountRepository,
            AuditService auditService) {
        this.adminRoleRepository = adminRoleRepository;
        this.adminPermissionRepository = adminPermissionRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.userAdminRoleRepository = userAdminRoleRepository;
        this.userAccountRepository = userAccountRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<AdminRoleResponse> listRoles() {
        return adminRoleRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminRoleResponse getRole(Long id) {
        return toResponse(requireRole(id));
    }

    @Transactional
    public AdminRoleResponse createRole(Long adminUserId, CreateAdminRoleRequest request) {
        if (adminRoleRepository.existsByName(request.name().trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Role name already exists");
        }
        AdminRole role = adminRoleRepository.save(
                new AdminRole(request.name().trim(), request.description(), false));

        if (request.permissionIds() != null) {
            for (Long permId : request.permissionIds()) {
                AdminPermission perm = requirePermission(permId);
                rolePermissionRepository.save(new RolePermission(role, perm));
            }
        }

        auditService.recordAdmin(adminUserId, "ADMIN_ROLE_CREATED", "ADMIN_ROLE", role.getId(),
                "name=" + role.getName());
        return toResponse(role);
    }

    @Transactional
    public AdminRoleResponse updateRole(Long adminUserId, Long id, UpdateAdminRoleRequest request) {
        AdminRole role = requireRole(id);
        if (role.isSystem()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot modify system role");
        }

        if (request.name() != null) {
            String name = request.name().trim();
            if (!name.equals(role.getName()) && adminRoleRepository.existsByName(name)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Role name already exists");
            }
            role.setName(name);
        }
        if (request.description() != null) {
            role.setDescription(request.description());
        }

        if (request.permissionIds() != null) {
            rolePermissionRepository.deleteByRole_Id(role.getId());
            for (Long permId : request.permissionIds()) {
                AdminPermission perm = requirePermission(permId);
                rolePermissionRepository.save(new RolePermission(role, perm));
            }
        }

        auditService.recordAdmin(adminUserId, "ADMIN_ROLE_UPDATED", "ADMIN_ROLE", role.getId(),
                "name=" + role.getName());
        return toResponse(role);
    }

    @Transactional
    public void deleteRole(Long adminUserId, Long id) {
        AdminRole role = requireRole(id);
        if (role.isSystem()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot delete system role");
        }
        rolePermissionRepository.deleteByRole_Id(role.getId());
        adminRoleRepository.delete(role);
        auditService.recordAdmin(adminUserId, "ADMIN_ROLE_DELETED", "ADMIN_ROLE", id, "name=" + role.getName());
    }

    @Transactional(readOnly = true)
    public List<AdminPermissionResponse> listPermissions() {
        return adminPermissionRepository.findAll().stream()
                .map(AdminPermissionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getUserRoles(Long userId) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        List<UserAdminRole> userRoles = userAdminRoleRepository.findAllByUser_Id(userId);
        List<Map<String, Object>> roleData = userRoles.stream()
                .map(ur -> {
                    Map<String, Object> r = new LinkedHashMap<>();
                    r.put("id", ur.getRole().getId());
                    r.put("name", ur.getRole().getName());
                    return r;
                })
                .toList();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", userId);
        result.put("roles", roleData);
        result.put("legacyPermissions", parseCsvPermissions(user.getAdminPermissions()));
        return result;
    }

    @Transactional
    public Map<String, Object> setUserRoles(Long adminUserId, Long userId, List<Long> roleIds) {
        UserAccount user = userAccountRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        userAdminRoleRepository.deleteByUser_Id(userId);
        if (roleIds != null) {
            for (Long roleId : roleIds) {
                AdminRole role = requireRole(roleId);
                userAdminRoleRepository.save(new UserAdminRole(user, role));
            }
        }
        auditService.recordAdmin(adminUserId, "ADMIN_USER_ROLES_UPDATED", "USER", userId,
                "roleIds=" + roleIds);
        return getUserRoles(userId);
    }

    public List<String> getUserEffectivePermissions(Long userId) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        List<String> permissions = new ArrayList<>(parseCsvPermissions(user.getAdminPermissions()));

        List<UserAdminRole> userRoles = userAdminRoleRepository.findAllByUser_Id(userId);
        for (UserAdminRole ur : userRoles) {
            List<RolePermission> rps = rolePermissionRepository.findAllByRole_Id(ur.getRole().getId());
            for (RolePermission rp : rps) {
                String code = rp.getPermission().getCode();
                if (!permissions.contains(code)) {
                    permissions.add(code);
                }
            }
        }
        return permissions;
    }

    private List<String> parseCsvPermissions(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(v -> !v.isBlank())
                .distinct()
                .toList();
    }

    private AdminRoleResponse toResponse(AdminRole role) {
        List<String> permissionCodes = rolePermissionRepository.findAllByRole_Id(role.getId())
                .stream()
                .map(rp -> rp.getPermission().getCode())
                .toList();
        return AdminRoleResponse.from(role, permissionCodes);
    }

    private AdminRole requireRole(Long id) {
        return adminRoleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found"));
    }

    private AdminPermission requirePermission(Long id) {
        return adminPermissionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Permission not found"));
    }
}
