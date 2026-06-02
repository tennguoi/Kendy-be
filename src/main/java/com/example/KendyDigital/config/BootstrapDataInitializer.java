package com.example.KendyDigital.config;



import com.example.KendyDigital.repository.*;
import com.example.KendyDigital.model.*;
import java.math.BigDecimal;
import java.util.Locale;

import org.springframework.boot.CommandLineRunner;
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

    public BootstrapDataInitializer(BootstrapProperties properties,
            UserAccountRepository userAccountRepository,
            ServiceItemRepository serviceItemRepository,
            PasswordEncoder passwordEncoder) {
        this.properties = properties;
        this.userAccountRepository = userAccountRepository;
        this.serviceItemRepository = serviceItemRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seedAdmin();
        seedServices();
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

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String blankToDefault(String value, String defaultValue) {
        return isBlank(value) ? defaultValue : value.trim();
    }
}
