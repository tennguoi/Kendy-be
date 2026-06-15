package com.example.KendyDigital.service.system;

import com.example.KendyDigital.config.SePayWebhookProperties;
import com.example.KendyDigital.dto.audit.response.AuditLogResponse;
import com.example.KendyDigital.dto.notification.response.AdminNotificationResponse;
import com.example.KendyDigital.dto.setting.request.SystemSettingsBulkUpdateRequest;
import com.example.KendyDigital.dto.setting.request.WebhookConfigRequest;
import com.example.KendyDigital.dto.setting.response.SystemSettingHistoryResponse;
import com.example.KendyDigital.dto.setting.response.SystemSettingResponse;
import com.example.KendyDigital.model.admin.AdminNotification;
import com.example.KendyDigital.model.setting.SystemSetting;
import com.example.KendyDigital.model.setting.SystemSettingHistory;
import com.example.KendyDigital.repository.AdminNotificationRepository;
import com.example.KendyDigital.repository.AuditLogRepository;
import com.example.KendyDigital.repository.SystemSettingHistoryRepository;
import com.example.KendyDigital.repository.SystemSettingRepository;
import com.example.KendyDigital.service.audit.AuditService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminSystemConfigServiceImpl  implements AdminSystemConfigService{
    private final SystemSettingRepository systemSettingRepository;
    private final SystemSettingHistoryRepository systemSettingHistoryRepository;
    private final AdminNotificationRepository adminNotificationRepository;
    private final AuditLogRepository auditLogRepository;
    private final AuditService auditService;
    private final SePayWebhookProperties sePayWebhookProperties;

    public AdminSystemConfigServiceImpl(SystemSettingRepository systemSettingRepository,
            SystemSettingHistoryRepository systemSettingHistoryRepository,
            AdminNotificationRepository adminNotificationRepository,
            AuditLogRepository auditLogRepository,
            AuditService auditService,
            SePayWebhookProperties sePayWebhookProperties) {
        this.systemSettingRepository = systemSettingRepository;
        this.systemSettingHistoryRepository = systemSettingHistoryRepository;
        this.adminNotificationRepository = adminNotificationRepository;
        this.auditLogRepository = auditLogRepository;
        this.auditService = auditService;
        this.sePayWebhookProperties = sePayWebhookProperties;
    }

    @Transactional(readOnly = true)
    public List<SystemSettingResponse> searchSettings(String query, Integer limit) {
        return systemSettingRepository.search(likePattern(normalizeQuery(query)), page(limit))
                .stream()
                .map(SystemSettingResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SystemSettingHistoryResponse> settingHistory(String key, Integer limit) {
        return systemSettingHistoryRepository.findAllByKeyOrderByCreatedAtDesc(key, page(limit))
                .stream()
                .map(SystemSettingHistoryResponse::from)
                .toList();
    }

    @Transactional
    public List<SystemSettingResponse> bulkUpdateSettings(Long adminUserId, SystemSettingsBulkUpdateRequest request) {
        return request.settings().stream()
                .map(item -> upsertSetting(adminUserId, item.key(), item.value(),
                        item.publicSetting() != null && item.publicSetting()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SystemSettingResponse> backupSettings() {
        return systemSettingRepository.findAll().stream()
                .map(SystemSettingResponse::from)
                .toList();
    }

    @Transactional
    public List<SystemSettingResponse> restoreSettings(Long adminUserId, SystemSettingsBulkUpdateRequest request) {
        List<SystemSettingResponse> restored = bulkUpdateSettings(adminUserId, request);
        auditService.recordAdmin(adminUserId, "SETTINGS_RESTORED", "SYSTEM_SETTING", null,
                "count=" + restored.size());
        return restored;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> sepayStatus() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("requireApiKey", sePayWebhookProperties.isRequireApiKey());
        response.put("apiKeyHeader", sePayWebhookProperties.getApiKeyHeader());
        response.put("requireHmac", sePayWebhookProperties.isRequireHmac());
        response.put("signatureHeader", sePayWebhookProperties.getSignatureHeader());
        response.put("recentWebhookLogs", auditLogRepository.searchAdmin(likePattern("SEPAY"), null, null, null, null, PageRequest.of(0, 10))
                .stream()
                .map(com.example.KendyDigital.dto.audit.response.AuditLogResponse::from)
                .toList());
        return response;
    }

    @Transactional(readOnly = true)
    public List<SystemSettingResponse> getSePayConfig() {
        return searchSettings("sepay.", 100);
    }

    @Transactional
    public List<SystemSettingResponse> updateSePayConfig(Long adminUserId, WebhookConfigRequest request) {
        if (request.config() == null) {
            return List.of();
        }
        return request.config().entrySet().stream()
                .map(entry -> upsertSetting(adminUserId, "sepay." + entry.getKey(), entry.getValue(), false))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminNotificationResponse> notifications(Long adminUserId, Integer limit) {
        return adminNotificationRepository.findAllByAdminUserIdIsNullOrAdminUserIdOrderByCreatedAtDesc(
                        adminUserId, page(limit))
                .stream()
                .map(AdminNotificationResponse::from)
                .toList();
    }

    @Transactional
    public AdminNotificationResponse markNotificationRead(Long adminUserId, Long id) {
        AdminNotification notification = adminNotificationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
        if (notification.getAdminUserId() != null && !notification.getAdminUserId().equals(adminUserId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found");
        }
        notification.markRead();
        return AdminNotificationResponse.from(notification);
    }

    @Transactional
    public List<AdminNotificationResponse> bulkReadNotifications(Long adminUserId, List<Long> ids) {
        return ids.stream()
                .map(id -> markNotificationRead(adminUserId, id))
                .toList();
    }

    @Transactional(readOnly = true)
    public SystemSettingResponse notificationSettings() {
        return systemSettingRepository.findById("notifications.settings")
                .map(SystemSettingResponse::from)
                .orElseGet(() -> new SystemSettingResponse("notifications.settings", "{}", false, null, null));
    }

    @Transactional
    public SystemSettingResponse updateNotificationSettings(Long adminUserId, String value) {
        return upsertSetting(adminUserId, "notifications.settings", value, false);
    }

    private SystemSettingResponse upsertSetting(Long adminUserId, String key, String value, boolean publicSetting) {
        SystemSetting setting = systemSettingRepository.findById(key).orElse(null);
        String oldValue = setting == null ? null : setting.getValue();
        if (setting == null) {
            setting = systemSettingRepository.save(new SystemSetting(key, value, publicSetting, adminUserId));
        } else {
            setting.update(value, publicSetting, adminUserId);
        }
        systemSettingHistoryRepository.save(new SystemSettingHistory(key, oldValue, value, adminUserId));
        auditService.recordAdmin(adminUserId, "SETTING_UPDATED", "SYSTEM_SETTING", null, "key=" + key);
        return SystemSettingResponse.from(setting);
    }

    private PageRequest page(Integer limit) {
        int normalizedLimit = limit == null ? 100 : Math.max(1, Math.min(limit, 500));
        return PageRequest.of(0, normalizedLimit);
    }

    private String normalizeQuery(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String likePattern(String value) {
        return value == null ? null : "%" + value.toLowerCase(java.util.Locale.ROOT) + "%";
    }
}
