package com.example.KendyDigital.service.order.helper;

import com.example.KendyDigital.common.CodeGenerator;
import com.example.KendyDigital.dto.notification.response.AdminNotificationResponse;
import com.example.KendyDigital.model.admin.AdminNotification;
import com.example.KendyDigital.model.order.OrderRecord;
import com.example.KendyDigital.model.ticket.Ticket;
import com.example.KendyDigital.model.ticket.TicketCategory;
import com.example.KendyDigital.model.ticket.TicketMessage;
import com.example.KendyDigital.model.ticket.TicketPriority;
import com.example.KendyDigital.model.ticket.TicketSenderRole;
import com.example.KendyDigital.model.user.UserAccount;
import com.example.KendyDigital.model.user.UserRole;
import com.example.KendyDigital.repository.AdminNotificationRepository;
import com.example.KendyDigital.repository.TicketMessageRepository;
import com.example.KendyDigital.repository.TicketRepository;
import com.example.KendyDigital.repository.UserAccountRepository;
import com.example.KendyDigital.service.audit.AuditService;
import com.example.KendyDigital.service.notification.EmailNotificationService;
import com.example.KendyDigital.service.notification.NotificationRealtimeService;
import java.util.List;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
public class ManualOrderSupportService {
    private final TicketRepository ticketRepository;
    private final TicketMessageRepository ticketMessageRepository;
    private final AdminNotificationRepository adminNotificationRepository;
    private final NotificationRealtimeService notificationRealtimeService;
    private final EmailNotificationService emailNotificationService;
    private final UserAccountRepository userAccountRepository;
    private final MessageSource messageSource;
    private final CodeGenerator codeGenerator;
    private final AuditService auditService;

    public ManualOrderSupportService(
            TicketRepository ticketRepository,
            TicketMessageRepository ticketMessageRepository,
            AdminNotificationRepository adminNotificationRepository,
            NotificationRealtimeService notificationRealtimeService,
            EmailNotificationService emailNotificationService,
            UserAccountRepository userAccountRepository,
            MessageSource messageSource,
            CodeGenerator codeGenerator,
            AuditService auditService) {
        this.ticketRepository = ticketRepository;
        this.ticketMessageRepository = ticketMessageRepository;
        this.adminNotificationRepository = adminNotificationRepository;
        this.notificationRealtimeService = notificationRealtimeService;
        this.emailNotificationService = emailNotificationService;
        this.userAccountRepository = userAccountRepository;
        this.messageSource = messageSource;
        this.codeGenerator = codeGenerator;
        this.auditService = auditService;
    }

    public Ticket createManualOrderTicket(OrderRecord order, UserAccount user) {
        Ticket ticket = ticketRepository.save(new Ticket(
                nextTicketCode(),
                user,
                order,
                null,
                TicketCategory.SERVICE,
                "Trao đổi đơn thủ công " + order.getOrderCode(),
                TicketPriority.NORMAL));
        String message = "Brief/yêu cầu từ đơn " + order.getOrderCode() + ":\n"
                + (order.getInputData() == null || order.getInputData().isBlank() ? "(không có brief)" : order.getInputData());
        ticketMessageRepository.save(new TicketMessage(ticket, user, TicketSenderRole.USER, message));
        auditService.recordSystem("MANUAL_ORDER_TICKET_CREATED", "TICKET", ticket.getId(),
                "orderId=" + order.getId());
        return ticket;
    }

    public void notifyAdminsManualOrder(OrderRecord order) {
        Locale defaultLocale = Locale.forLanguageTag("vi");
        String title = messageSource.getMessage("admin.notification.order.manual_new.title",
                new Object[]{order.getOrderCode()}, defaultLocale);
        String message = messageSource.getMessage("admin.notification.order.manual_new.body",
                new Object[]{order.getUser().getId(), order.getService().getName()}, defaultLocale);
        AdminNotification notification = adminNotificationRepository.save(new AdminNotification(null, title, message));
        notificationRealtimeService.publishAdminNotification(AdminNotificationResponse.from(notification));
        userAccountRepository.findAllByRoleInOrderByCreatedAtDesc(
                        List.of(UserRole.ADMIN, UserRole.SUPER_ADMIN),
                        PageRequest.of(0, 50))
                .forEach(admin -> emailNotificationService.sendUserNotification(admin, title, message,
                        "/admin/orders"));
    }

    private String nextTicketCode() {
        String code;
        do {
            code = codeGenerator.generate("TK", 10);
        } while (ticketRepository.existsByTicketCode(code));
        return code;
    }
}
