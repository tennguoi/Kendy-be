package com.example.KendyDigital.controller;

import com.example.KendyDigital.config.AppEmailProperties;
import com.example.KendyDigital.dto.email.request.PublicConsultRequest;
import com.example.KendyDigital.service.notification.EmailNotificationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/consult")
public class PublicConsultController {
    private static final Logger LOGGER = LoggerFactory.getLogger(PublicConsultController.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy");

    private final EmailNotificationService emailNotificationService;
    private final AppEmailProperties emailProperties;

    public PublicConsultController(EmailNotificationService emailNotificationService,
                                   AppEmailProperties emailProperties) {
        this.emailNotificationService = emailNotificationService;
        this.emailProperties = emailProperties;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> submitConsult(
            @Valid @RequestBody PublicConsultRequest request,
            HttpServletRequest servletRequest) {

        String recipient = resolveRecipientEmail();
        String clientIp = clientIp(servletRequest);
        String submitTime = LocalDateTime.now().format(DATE_TIME_FORMATTER);

        String safeName = StringEscapeUtils.escapeHtml4(request.name().trim());
        String safePhone = StringEscapeUtils.escapeHtml4(request.phone().trim());
        String safeIndustry = request.industry() != null && !request.industry().isBlank()
                ? StringEscapeUtils.escapeHtml4(request.industry().trim())
                : "Chưa cung cấp";
        String safeBudget = request.budget() != null && !request.budget().isBlank()
                ? StringEscapeUtils.escapeHtml4(request.budget().trim())
                : "Chưa xác định";
        String safeGoal = StringEscapeUtils.escapeHtml4(request.goal().trim());

        String subject = String.format("[KendyDigital] Yêu cầu tư vấn mới từ: %s (%s)", safeName, safePhone);

        String htmlContent = String.format("""
            <!DOCTYPE html>
            <html lang="vi">
            <head>
              <meta charset="UTF-8">
              <style>
                body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f4f6f8; margin: 0; padding: 20px; color: #333; }
                .container { max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.08); }
                .header { background: linear-gradient(135deg, #1e3c72 0%%, #2a5298 100%%); color: #ffffff; padding: 24px; text-align: center; }
                .header h2 { margin: 0 0 6px 0; font-size: 22px; }
                .header p { margin: 0; font-size: 14px; opacity: 0.85; }
                .content { padding: 24px; }
                .table { width: 100%%; border-collapse: collapse; margin-top: 10px; }
                .table th, .table td { padding: 12px 14px; text-align: left; font-size: 14px; border-bottom: 1px solid #eef0f3; }
                .table th { width: 35%%; color: #64748b; font-weight: 600; background: #f8fafc; }
                .table td { color: #1e293b; font-weight: 500; }
                .goal-box { margin-top: 16px; padding: 14px; background: #f8fafc; border-left: 4px solid #2a5298; border-radius: 4px; font-size: 14px; line-height: 1.6; white-space: pre-line; }
                .footer { background: #f1f5f9; padding: 14px 24px; text-align: center; font-size: 12px; color: #94a3b8; border-top: 1px solid #e2e8f0; }
              </style>
            </head>
            <body>
              <div class="container">
                <div class="header">
                  <h2>🚀 Yêu Cầu Tư Vấn Khách Hàng Mới</h2>
                  <p>Nhận lúc %s từ Website KendyDigital</p>
                </div>
                <div class="content">
                  <table class="table">
                    <tr>
                      <th>Họ và tên</th>
                      <td><strong>%s</strong></td>
                    </tr>
                    <tr>
                      <th>Số điện thoại / Zalo</th>
                      <td><a href="tel:%s" style="color: #2563eb; text-decoration: none; font-weight: bold;">%s</a></td>
                    </tr>
                    <tr>
                      <th>Ngành hàng</th>
                      <td>%s</td>
                    </tr>
                    <tr>
                      <th>Ngân sách dự kiến</th>
                      <td>%s</td>
                    </tr>
                    <tr>
                      <th>Địa chỉ IP gửi</th>
                      <td>%s</td>
                    </tr>
                  </table>
                  <h4 style="margin-top: 20px; margin-bottom: 8px; color: #1e293b;">Mục tiêu cần tư vấn:</h4>
                  <div class="goal-box">%s</div>
                </div>
                <div class="footer">
                  Email tự động từ hệ thống KendyDigital • Vui lòng liên hệ lại khách hàng sớm nhất có thể.
                </div>
              </div>
            </body>
            </html>
            """,
                submitTime, safeName, safePhone, safePhone, safeIndustry, safeBudget, clientIp, safeGoal
        );

        LOGGER.info("Sending public consult notification email to {} for customer {} ({})", recipient, safeName, safePhone);
        emailNotificationService.sendRawEmail(recipient, subject, htmlContent);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Yêu cầu tư vấn của bạn đã được gửi thành công! Đội ngũ KendyDigital sẽ liên hệ lại sớm nhất."
        ));
    }

    private String resolveRecipientEmail() {
        if (emailProperties.getConsultRecipient() != null && !emailProperties.getConsultRecipient().isBlank()) {
            return emailProperties.getConsultRecipient().trim();
        }
        if (emailProperties.getFrom() != null && !emailProperties.getFrom().isBlank()) {
            return emailProperties.getFrom().trim();
        }
        return "admin@kendydigital.local";
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
