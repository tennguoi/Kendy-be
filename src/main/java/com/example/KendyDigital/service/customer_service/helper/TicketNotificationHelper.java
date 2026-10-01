package com.example.KendyDigital.service.customer_service.helper;

import com.example.KendyDigital.dto.notification.response.AdminNotificationResponse;
import com.example.KendyDigital.model.admin.AdminNotification;
import com.example.KendyDigital.model.ticket.Ticket;
import com.example.KendyDigital.model.user.UserAccount;
import com.example.KendyDigital.model.user.UserRole;
import com.example.KendyDigital.repository.AdminNotificationRepository;
import com.example.KendyDigital.repository.UserAccountRepository;
import com.example.KendyDigital.service.notification.EmailNotificationService;
import com.example.KendyDigital.service.notification.NotificationRealtimeService;
import com.example.KendyDigital.service.notification.UserNotificationService;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TicketNotificationHelper {
    private final AdminNotificationRepository adminNotificationRepository;
    private final NotificationRealtimeService notificationRealtimeService;
    private final UserNotificationService userNotificationService;
    private final EmailNotificationService emailNotificationService;
    private final UserAccountRepository userAccountRepository;

    public TicketNotificationHelper(
            AdminNotificationRepository adminNotificationRepository,
            NotificationRealtimeService notificationRealtimeService,
            UserNotificationService userNotificationService,
            EmailNotificationService emailNotificationService,
            UserAccountRepository userAccountRepository) {
        this.adminNotificationRepository = adminNotificationRepository;
        this.notificationRealtimeService = notificationRealtimeService;
        this.userNotificationService = userNotificationService;
        this.emailNotificationService = emailNotificationService;
        this.userAccountRepository = userAccountRepository;
    }

    public void notifyNewTicket(Ticket ticket, UserAccount user) {
        AdminNotification notification = adminNotificationRepository.save(new AdminNotification(null,
                "New ticket: " + ticket.getSubject(),
                "Ticket " + ticket.getTicketCode() + " created by " + user.getEmail()));
        notificationRealtimeService.publishAdminNotification(AdminNotificationResponse.from(notification));

        List<UserAccount> admins = userAccountRepository.findByRoleIn(List.of(UserRole.ADMIN, UserRole.SUPER_ADMIN));
        for (UserAccount admin : admins) {
            emailNotificationService.sendUserNotification(admin,
                    "New ticket: " + ticket.getSubject(),
                    "Ticket " + ticket.getTicketCode() + " created by " + user.getEmail(),
                    "/admin/tickets/" + ticket.getTicketCode());
        }
    }

    public void notifyTicketReopened(Ticket ticket) {
        AdminNotification notification = adminNotificationRepository.save(new AdminNotification(null,
                "Ticket reopened: " + ticket.getSubject(),
                "Ticket " + ticket.getTicketCode() + " reopened by user"));
        notificationRealtimeService.publishAdminNotification(AdminNotificationResponse.from(notification));
    }

    public void notifyUserTicketReplied(Ticket ticket) {
        userNotificationService.create(ticket.getUser().getId(),
                "Ticket replied: " + ticket.getSubject(),
                "Admin replied to ticket " + ticket.getTicketCode(),
                "TICKET",
                "/tickets/" + ticket.getTicketCode());
    }

    public void notifyUserTicketUpdated(Ticket ticket) {
        userNotificationService.create(ticket.getUser().getId(),
                "Ticket updated: " + ticket.getSubject(),
                "Ticket " + ticket.getTicketCode() + " status is " + ticket.getStatus(),
                "TICKET",
                "/tickets/" + ticket.getTicketCode());
    }
}
