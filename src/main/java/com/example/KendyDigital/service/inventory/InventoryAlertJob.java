package com.example.KendyDigital.service.inventory;

import com.example.KendyDigital.dto.inventory.response.InventoryAlertSummaryResponse;
import com.example.KendyDigital.model.admin.AdminNotification;
import com.example.KendyDigital.model.user.UserRole;
import com.example.KendyDigital.repository.AdminNotificationRepository;
import com.example.KendyDigital.repository.UserAccountRepository;
import com.example.KendyDigital.service.notification.EmailNotificationService;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryAlertJob {
    private final AccountInventoryService accountInventoryService;
    private final AdminNotificationRepository adminNotificationRepository;
    private final UserAccountRepository userAccountRepository;
    private final EmailNotificationService emailNotificationService;
    private final MessageSource messageSource;
    private final boolean enabled;
    private final int lowStockThreshold;
    private final int expiringDays;

    public InventoryAlertJob(AccountInventoryService accountInventoryService,
            AdminNotificationRepository adminNotificationRepository,
            UserAccountRepository userAccountRepository,
            EmailNotificationService emailNotificationService,
            MessageSource messageSource,
            @Value("${app.inventory.alert-enabled:true}") boolean enabled,
            @Value("${app.inventory.low-stock-threshold:2}") int lowStockThreshold,
            @Value("${app.inventory.expiring-days:7}") int expiringDays) {
        this.accountInventoryService = accountInventoryService;
        this.adminNotificationRepository = adminNotificationRepository;
        this.userAccountRepository = userAccountRepository;
        this.emailNotificationService = emailNotificationService;
        this.messageSource = messageSource;
        this.enabled = enabled;
        this.lowStockThreshold = lowStockThreshold;
        this.expiringDays = expiringDays;
    }

    @Transactional
    @Scheduled(cron = "${app.inventory.alert-cron:0 0 8 * * *}")
    public void sendInventoryAlerts() {
        if (!enabled) {
            return;
        }
        InventoryAlertSummaryResponse summary =
                accountInventoryService.alertSummary(lowStockThreshold, expiringDays);
        if (summary.lowStockServices() <= 0 && summary.expiringCredentials() <= 0
                && summary.reserveReleased() <= 0) {
            return;
        }

        Locale defaultLocale = Locale.forLanguageTag("vi");
        String title = messageSource.getMessage("admin.notification.inventory.alert.title",
                null, defaultLocale);
        String message = messageSource.getMessage("admin.notification.inventory.alert.body",
                new Object[]{summary.lowStockServices(), expiringDays,
                        summary.expiringCredentials(), summary.reserveReleased()},
                defaultLocale);
        adminNotificationRepository.save(new AdminNotification(null, title, message));
        userAccountRepository.findAllByRoleInOrderByCreatedAtDesc(
                        List.of(UserRole.ADMIN, UserRole.SUPER_ADMIN),
                        PageRequest.of(0, 50))
                .forEach(admin -> emailNotificationService.sendUserNotification(admin, title, message,
                        "/admin/services"));
    }
}
