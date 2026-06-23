package com.example.KendyDigital.service.role;

import com.example.KendyDigital.dto.role.request.CreateAdminRoleRequest;
import com.example.KendyDigital.dto.role.request.UpdateAdminRoleRequest;
import com.example.KendyDigital.dto.role.response.AdminPermissionResponse;
import com.example.KendyDigital.dto.role.response.AdminRoleResponse;
import com.example.KendyDigital.model.admin.AdminPermission;
import com.example.KendyDigital.model.admin.AdminRole;
import com.example.KendyDigital.model.admin.RolePermission;
import com.example.KendyDigital.model.user.UserAccount;
import com.example.KendyDigital.model.user.UserAdminRole;
import com.example.KendyDigital.repository.AdminPermissionRepository;
import com.example.KendyDigital.repository.AdminRoleRepository;
import com.example.KendyDigital.repository.RolePermissionRepository;
import com.example.KendyDigital.repository.UserAccountRepository;
import com.example.KendyDigital.repository.UserAdminRoleRepository;
import com.example.KendyDigital.service.audit.AuditService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminRoleServiceImpl  implements AdminRoleService{
    private final AdminRoleRepository adminRoleRepository;
    private final AdminPermissionRepository adminPermissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserAdminRoleRepository userAdminRoleRepository;
    private final UserAccountRepository userAccountRepository;
    private final AuditService auditService;

    public AdminRoleServiceImpl(AdminRoleRepository adminRoleRepository,
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
    public List<AdminRoleResponse> listRoles(Integer limit) {
        int normalizedLimit = limit == null ? 100 : Math.max(1, Math.min(limit, 200));
        List<AdminRole> roles = adminRoleRepository.findAll(PageRequest.of(0, normalizedLimit)).getContent();
        Map<Long, List<String>> permissionsByRole = roles.isEmpty()
                ? Map.of()
                : rolePermissionRepository
                        .findAllByRole_IdInWithPermission(roles.stream().map(AdminRole::getId).toList())
                        .stream()
                        .collect(Collectors.groupingBy(
                                rp -> rp.getRole().getId(),
                                Collectors.mapping(rp -> rp.getPermission().getCode(), Collectors.toList())));
        return roles.stream()
                .map(role -> AdminRoleResponse.from(role,
                        permissionsByRole.getOrDefault(role.getId(), List.of())))
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
            rolePermissionRepository.saveAll(resolvePermissions(request.permissionIds()).stream()
                    .map(permission -> new RolePermission(role, permission))
                    .toList());
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
            rolePermissionRepository.saveAll(resolvePermissions(request.permissionIds()).stream()
                    .map(permission -> new RolePermission(role, permission))
                    .toList());
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
        if (userAdminRoleRepository.existsByRole_Id(role.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Role is still assigned to users");
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

        rolePermissionRepository.findPermissionCodesByUserId(userId).stream()
                .filter(code -> !permissions.contains(code))
                .forEach(permissions::add);
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

    private List<AdminPermission> resolvePermissions(List<Long> permissionIds) {
        List<Long> distinctIds = permissionIds.stream().distinct().toList();
        List<AdminPermission> permissions = adminPermissionRepository.findAllById(distinctIds);
        if (permissions.size() != distinctIds.size()) {
            Set<Long> foundIds = permissions.stream().map(AdminPermission::getId).collect(Collectors.toSet());
            Long missingId = distinctIds.stream().filter(id -> !foundIds.contains(id)).findFirst().orElse(null);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Permission not found: " + missingId);
        }
        return permissions;
    }
}
