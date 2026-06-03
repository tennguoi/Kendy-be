package com.example.KendyDigital.config;



import com.example.KendyDigital.repository.*;
import com.example.KendyDigital.model.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

import org.springframework.boot.CommandLineRunner;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.KendyDigital.model.UserAccount;
import com.example.KendyDigital.repository.UserAccountRepository;
import com.example.KendyDigital.model.UserRole;
import com.example.KendyDigital.model.ServiceItem;
import com.example.KendyDigital.repository.ServiceItemRepository;
import com.example.KendyDigital.model.ServiceStatus;
import com.example.KendyDigital.model.ServiceType;

@Component
public class BootstrapDataInitializer implements CommandLineRunner {
    private final BootstrapProperties properties;
    private final UserAccountRepository userAccountRepository;
    private final ServiceItemRepository serviceItemRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminRoleRepository adminRoleRepository;
    private final AdminPermissionRepository adminPermissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final ServiceCategoryRepository serviceCategoryRepository;
    private final UserAdminRoleRepository userAdminRoleRepository;

    public BootstrapDataInitializer(BootstrapProperties properties,
            UserAccountRepository userAccountRepository,
            ServiceItemRepository serviceItemRepository,
            PasswordEncoder passwordEncoder,
            AdminRoleRepository adminRoleRepository,
            AdminPermissionRepository adminPermissionRepository,
            RolePermissionRepository rolePermissionRepository,
            ServiceCategoryRepository serviceCategoryRepository,
            UserAdminRoleRepository userAdminRoleRepository) {
        this.properties = properties;
        this.userAccountRepository = userAccountRepository;
        this.serviceItemRepository = serviceItemRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminRoleRepository = adminRoleRepository;
        this.adminPermissionRepository = adminPermissionRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.serviceCategoryRepository = serviceCategoryRepository;
        this.userAdminRoleRepository = userAdminRoleRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seedAdmin();
        seedPermissions();
        seedRoles();
        seedAdminRoles();
        seedCategories();
        seedServices();
    }

    private void seedAdminRoles() {
        if (adminRoleRepository.count() == 0) {
            return;
        }
        List<UserAccount> admins = userAccountRepository.findAllByRoleInOrderByCreatedAtDesc(
                List.of(UserRole.ADMIN, UserRole.SUPER_ADMIN), Pageable.unpaged());
        for (UserAccount admin : admins) {
            if (!userAdminRoleRepository.findAllByUser_Id(admin.getId()).isEmpty()) {
                continue;
            }
            String roleName = admin.getRole() == UserRole.SUPER_ADMIN ? "Super Admin" : "Admin";
            adminRoleRepository.findByName(roleName).ifPresent(role ->
                    userAdminRoleRepository.save(new UserAdminRole(admin, role)));
        }
    }

    private void seedAdmin() {
        if (isBlank(properties.getAdminEmail()) && isBlank(properties.getAdminPassword())) {
            return;
        }
        if (isBlank(properties.getAdminEmail()) || isBlank(properties.getAdminPassword())) {
            throw new IllegalStateException("Both bootstrap admin email and password are required");
        }
        if (properties.getAdminPassword().length() < 12) {
            throw new IllegalStateException("Bootstrap admin password must be at least 12 characters");
        }

        String email = properties.getAdminEmail().trim().toLowerCase(Locale.ROOT);
        if (userAccountRepository.existsByEmailIgnoreCase(email)) {
            return;
        }

        UserAccount admin = new UserAccount(
                blankToDefault(properties.getAdminName(), "Kendy Admin"),
                email,
                null,
                passwordEncoder.encode(properties.getAdminPassword()));
        admin.setRole(UserRole.SUPER_ADMIN);
        userAccountRepository.save(admin);
    }

    private void seedServices() {
        if (!properties.isSeedServices() || serviceItemRepository.count() > 0) {
            return;
        }

        serviceItemRepository.save(new ServiceItem(
                "AI API Credit",
                "ai-api-credit",
                "Goi credit AI dung cho API va dashboard usage.",
                "Nap credit de su dung API AI, theo doi lich su tru vi va usage minh bach.",
                BigDecimal.valueOf(250000).setScale(2),
                ServiceType.API_CREDIT,
                ServiceStatus.ACTIVE));

        serviceItemRepository.save(new ServiceItem(
                "Digital Growth Pack",
                "digital-growth-pack",
                "Goi dich vu so can admin xu ly va cap nhat ket qua.",
                "Phu hop cho cac tac vu digital service, ban tai nguyen va quy trinh manual co bao hanh.",
                BigDecimal.valueOf(490000).setScale(2),
                ServiceType.MANUAL,
                ServiceStatus.ACTIVE));

        serviceItemRepository.save(new ServiceItem(
                "Automation Setup",
                "automation-setup",
                "Thiet lap workflow, webhook va tich hop provider.",
                "Goi tu van va trien khai automation cho shop dich vu so/API/subscription.",
                BigDecimal.valueOf(890000).setScale(2),
                ServiceType.MANUAL,
                ServiceStatus.MAINTENANCE));
    }

    private void seedPermissions() {
        if (adminPermissionRepository.count() > 0) {
            return;
        }
        List.of(
                new AdminPermission("ORDER_VIEW", "View orders", "ORDER"),
                new AdminPermission("ORDER_EDIT", "Edit/update orders", "ORDER"),
                new AdminPermission("ORDER_REFUND", "Refund orders", "ORDER"),
                new AdminPermission("ORDER_COMPLETE", "Complete orders", "ORDER"),
                new AdminPermission("ORDER_CANCEL", "Cancel orders", "ORDER"),
                new AdminPermission("USER_VIEW", "View users", "USER"),
                new AdminPermission("USER_EDIT", "Edit users", "USER"),
                new AdminPermission("USER_LOCK", "Lock/unlock users", "USER"),
                new AdminPermission("USER_ROLE", "Change user roles", "USER"),
                new AdminPermission("WALLET_ADJUST", "Adjust wallet balance", "WALLET"),
                new AdminPermission("WALLET_VIEW", "View wallet transactions", "WALLET"),
                new AdminPermission("BANK_VIEW", "View bank transactions", "BANK"),
                new AdminPermission("BANK_CREDIT", "Manual credit bank transactions", "BANK"),
                new AdminPermission("BANK_IGNORE", "Ignore bank transactions", "BANK"),
                new AdminPermission("DEPOSIT_VIEW", "View deposits", "DEPOSIT"),
                new AdminPermission("DEPOSIT_EDIT", "Edit deposits", "DEPOSIT"),
                new AdminPermission("TICKET_VIEW", "View tickets", "TICKET"),
                new AdminPermission("TICKET_REPLY", "Reply to tickets", "TICKET"),
                new AdminPermission("SERVICE_VIEW", "View services", "SERVICE"),
                new AdminPermission("SERVICE_EDIT", "Edit services", "SERVICE"),
                new AdminPermission("SETTINGS_VIEW", "View settings", "SETTINGS"),
                new AdminPermission("SETTINGS_EDIT", "Edit settings", "SETTINGS"),
                new AdminPermission("REPORT_VIEW", "View reports", "REPORT"),
                new AdminPermission("REPORT_EXPORT", "Export reports", "REPORT"),
                new AdminPermission("ADMIN_MANAGE", "Manage admin users", "ADMIN"),
                new AdminPermission("AUDIT_VIEW", "View audit logs", "AUDIT"),
                new AdminPermission("NOTIFICATION_MANAGE", "Manage notifications", "NOTIFICATION"),
                new AdminPermission("FILE_MANAGE", "Upload/delete files", "FILE"),
                new AdminPermission("JOB_MANAGE", "Manage jobs", "JOB"))
                .forEach(adminPermissionRepository::save);
    }

    private void seedRoles() {
        if (adminRoleRepository.count() > 0) {
            return;
        }
        AdminRole superAdmin = adminRoleRepository.save(
                new AdminRole("Super Admin", "Full system access", true));
        AdminRole admin = adminRoleRepository.save(
                new AdminRole("Admin", "Standard admin access", true));

        List<AdminPermission> allPerms = adminPermissionRepository.findAll();
        for (AdminPermission perm : allPerms) {
            rolePermissionRepository.save(new RolePermission(superAdmin, perm));
        }

        List.of("ORDER_VIEW", "ORDER_EDIT", "ORDER_REFUND", "ORDER_COMPLETE", "ORDER_CANCEL",
                "USER_VIEW", "USER_EDIT", "USER_LOCK",
                "WALLET_VIEW", "WALLET_ADJUST",
                "BANK_VIEW", "BANK_CREDIT",
                "DEPOSIT_VIEW", "DEPOSIT_EDIT",
                "TICKET_VIEW", "TICKET_REPLY",
                "SERVICE_VIEW", "SERVICE_EDIT",
                "REPORT_VIEW", "REPORT_EXPORT",
                "AUDIT_VIEW", "NOTIFICATION_MANAGE", "FILE_MANAGE")
                .forEach(code -> {
                    adminPermissionRepository.findByCode(code).ifPresent(
                            perm -> rolePermissionRepository.save(new RolePermission(admin, perm)));
                });
    }

    private void seedCategories() {
        if (serviceCategoryRepository.count() > 0) {
            return;
        }
        serviceCategoryRepository.save(new ServiceCategory(
                "AI & API", "ai-api", "AI services and API credits", 1));
        serviceCategoryRepository.save(new ServiceCategory(
                "Digital Marketing", "digital-marketing", "Digital growth and marketing services", 2));
        serviceCategoryRepository.save(new ServiceCategory(
                "Automation", "automation", "Workflow automation and integration", 3));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String blankToDefault(String value, String defaultValue) {
        return isBlank(value) ? defaultValue : value.trim();
    }
}
