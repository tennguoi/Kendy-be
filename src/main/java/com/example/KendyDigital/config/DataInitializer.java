package com.example.KendyDigital.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.KendyDigital.model.admin.AdminRole;
import com.example.KendyDigital.model.user.UserAccount;
import com.example.KendyDigital.model.user.UserAdminRole;
import com.example.KendyDigital.model.user.UserRole;
import com.example.KendyDigital.repository.AdminRoleRepository;
import com.example.KendyDigital.repository.UserAccountRepository;
import com.example.KendyDigital.repository.UserAdminRoleRepository;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserAccountRepository userAccountRepository;
    private final AdminRoleRepository adminRoleRepository;
    private final UserAdminRoleRepository userAdminRoleRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserAccountRepository userAccountRepository,
            AdminRoleRepository adminRoleRepository,
            UserAdminRoleRepository userAdminRoleRepository,
            PasswordEncoder passwordEncoder) {
        this.userAccountRepository = userAccountRepository;
        this.adminRoleRepository = adminRoleRepository;
        this.userAdminRoleRepository = userAdminRoleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        AdminRole adminRole = createRoleIfNotExists("admin", "Administrator role with full system access");

        userAccountRepository.findByEmailIgnoreCase("nguyenduccongminh34@gmail.com")
                .ifPresentOrElse(admin -> {
                    if (!passwordEncoder.matches("admin123", admin.getPasswordHash())) {
                        admin.changePasswordHash(passwordEncoder.encode("admin123"));
                        userAccountRepository.save(admin);
                    }
                }, () -> {
                    UserAccount admin = new UserAccount(
                            "Nguyễn Văn Admin",
                            "nguyenduccongminh34@gmail.com",
                            "0123456789",
                            passwordEncoder.encode("admin123"));
                    admin.setRole(UserRole.SUPER_ADMIN);
                    admin.verifyEmail();
                    admin = userAccountRepository.save(admin);

                    UserAdminRole adminUserRole = new UserAdminRole(admin, adminRole);
                    userAdminRoleRepository.save(adminUserRole);
                });

        userAccountRepository.findByEmailIgnoreCase("user@gmail.com")
                .ifPresentOrElse(user -> {
                    if (!passwordEncoder.matches("user123", user.getPasswordHash())) {
                        user.changePasswordHash(passwordEncoder.encode("user123"));
                        userAccountRepository.save(user);
                    }
                }, () -> {
                    UserAccount user = new UserAccount(
                            "Nguyễn Văn User",
                            "user@gmail.com",
                            "0901234567",
                            passwordEncoder.encode("user123"));
                    user.setRole(UserRole.USER);
                    user.verifyEmail();
                    userAccountRepository.save(user);
                });
    }

    private AdminRole createRoleIfNotExists(String name, String description) {
        return adminRoleRepository.findByName(name)
                .orElseGet(() -> {
                    AdminRole role = new AdminRole(name, description, true);
                    return adminRoleRepository.save(role);
                });
    }
}
