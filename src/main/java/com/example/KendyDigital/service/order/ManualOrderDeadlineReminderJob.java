package com.example.KendyDigital.service.order;

import com.example.KendyDigital.dto.notification.response.AdminNotificationResponse;
import com.example.KendyDigital.model.admin.AdminNotification;
import com.example.KendyDigital.model.order.OrderRecord;
import com.example.KendyDigital.model.user.UserAccount;
import com.example.KendyDigital.model.user.UserRole;
import com.example.KendyDigital.repository.AdminNotificationRepository;
import com.example.KendyDigital.repository.OrderRepository;
import com.example.KendyDigital.repository.UserAccountRepository;
import com.example.KendyDigital.service.notification.EmailNotificationService;
import com.example.KendyDigital.service.notification.NotificationRealtimeService;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ManualOrderDeadlineReminderJob {
    private final OrderRepository orderRepository;
    private final AdminNotificationRepository adminNotificationRepository;
    private final UserAccountRepository userAccountRepository;
    private final EmailNotificationService emailNotificationService;
    private final NotificationRealtimeService notificationRealtimeService;
    private final MessageSource messageSource;

    public ManualOrderDeadlineReminderJob(OrderRepository orderRepository,
            AdminNotificationRepository adminNotificationRepository,
            UserAccountRepository userAccountRepository,
            EmailNotificationService emailNotificationService,
            NotificationRealtimeService notificationRealtimeService,
            MessageSource messageSource) {
        this.orderRepository = orderRepository;
        this.adminNotificationRepository = adminNotificationRepository;
        this.userAccountRepository = userAccountRepository;
        this.emailNotificationService = emailNotificationService;
        this.notificationRealtimeService = notificationRealtimeService;
        this.messageSource = messageSource;
    }

    @Scheduled(fixedDelayString = "${app.manual-order.deadline-check-interval-ms:60000}")
    @Transactional
    public void run() {
        Instant now = Instant.now();
        Instant warningTime = now.plus(Duration.ofMinutes(30));
        orderRepository.findManualOrdersDueSoon(now, warningTime, PageRequest.of(0, 100))
                .forEach(order -> {
                    notifyDeadline(order, false);
                    order.markDeadlineReminderSent();
                });
        orderRepository.findManualOrdersOverdue(now, PageRequest.of(0, 100))
                .forEach(order -> {
                    notifyDeadline(order, true);
                    order.markDeadlineOverdueNotified();
                });
    }

    private void notifyDeadline(OrderRecord order, boolean overdue) {
        Locale defaultLocale = Locale.forLanguageTag("vi");
        String titleKey = overdue
                ? "admin.notification.order.deadline_overdue.title"
                : "admin.notification.order.deadline_soon.title";
        String title = messageSource.getMessage(titleKey,
                new Object[]{order.getOrderCode()}, defaultLocale);
        String message = messageSource.getMessage("admin.notification.order.deadline.body",
                new Object[]{order.getService().getName(), order.getUser().getId(),
                        order.getProcessingDeadlineAt()},
                defaultLocale);
        Long targetAdminId = order.getAssignedAdmin() == null ? null : order.getAssignedAdmin().getId();
        AdminNotification notification = adminNotificationRepository.save(new AdminNotification(targetAdminId, title, message));
        notificationRealtimeService.publishAdminNotification(AdminNotificationResponse.from(notification));

        List<UserAccount> admins = order.getAssignedAdmin() != null
                ? List.of(order.getAssignedAdmin())
                : userAccountRepository.findAllByRoleInOrderByCreatedAtDesc(
                        List.of(UserRole.ADMIN, UserRole.SUPER_ADMIN),
                        PageRequest.of(0, 50));
        admins.forEach(admin -> emailNotificationService.sendUserNotification(admin, title, message,
                "/admin/orders"));
    }
}
