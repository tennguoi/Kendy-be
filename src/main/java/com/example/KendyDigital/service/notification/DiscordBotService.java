package com.example.KendyDigital.service.notification;

import java.math.BigDecimal;

public interface DiscordBotService {
    void sendAlert(String title, String description, int colorHex);
    void sendOrderDeadlineAlert(String orderCode, String serviceName, Long userId, String deadline, boolean overdue);
    void sendSecurityAlert(String severity, String ruleCode, String title, String detail);
    void sendDepositAlert(String depositCode, BigDecimal amount, String customerName, String accountNumber, String transactionRef);
    void sendNewOrderAlert(String orderCode, String serviceName, Long userId, String price, boolean isManual);
    boolean isReady();
}
