package com.example.KendyDigital.controller;

import com.example.KendyDigital.dto.notification.request.BulkReadNotificationsRequest;
import com.example.KendyDigital.dto.notification.response.AdminNotificationResponse;
import com.example.KendyDigital.dto.setting.request.SystemSettingUpdateRequest;
import com.example.KendyDigital.dto.setting.request.SystemSettingsBulkUpdateRequest;
import com.example.KendyDigital.dto.setting.request.WebhookConfigRequest;
import com.example.KendyDigital.dto.setting.response.SystemSettingHistoryResponse;
import com.example.KendyDigital.dto.setting.response.SystemSettingResponse;
import com.example.KendyDigital.security.CurrentUser;
import com.example.KendyDigital.service.system.AdminSystemConfigService;
import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class AdminSystemConfigController {
    private final AdminSystemConfigService systemConfigService;

    public AdminSystemConfigController(AdminSystemConfigService systemConfigService) {
        this.systemConfigService = systemConfigService;
    }

    @GetMapping("/api/admin/settings/search")
    public List<SystemSettingResponse> searchSettings(@RequestParam(required = false) String query,
            @RequestParam(required = false) Integer limit) {
        return systemConfigService.searchSettings(query, limit);
    }

    @GetMapping("/api/admin/settings/{key}/history")
    public List<SystemSettingHistoryResponse> settingHistory(@PathVariable String key,
            @RequestParam(required = false) Integer limit) {
        return systemConfigService.settingHistory(key, limit);
    }

    @PutMapping("/api/admin/settings/bulk-update")
    public List<SystemSettingResponse> bulkUpdateSettings(Authentication authentication,
            @Valid @RequestBody SystemSettingsBulkUpdateRequest request) {
        return systemConfigService.bulkUpdateSettings(CurrentUser.require(authentication).userId(), request);
    }

    @PostMapping("/api/admin/settings/backup")
    public List<SystemSettingResponse> backupSettings() {
        return systemConfigService.backupSettings();
    }

    @PostMapping("/api/admin/settings/restore")
    public List<SystemSettingResponse> restoreSettings(Authentication authentication,
            @Valid @RequestBody SystemSettingsBulkUpdateRequest request) {
        return systemConfigService.restoreSettings(CurrentUser.require(authentication).userId(), request);
    }

    @GetMapping("/api/admin/webhooks/sepay/status")
    public Map<String, Object> sepayStatus() {
        return systemConfigService.sepayStatus();
    }

    @GetMapping("/api/admin/webhooks/sepay/config")
    public List<SystemSettingResponse> getSePayConfig() {
        return systemConfigService.getSePayConfig();
    }

    @PutMapping("/api/admin/webhooks/sepay/config")
    public List<SystemSettingResponse> updateSePayConfig(Authentication authentication,
            @Valid @RequestBody WebhookConfigRequest request) {
        return systemConfigService.updateSePayConfig(CurrentUser.require(authentication).userId(), request);
    }

    @GetMapping("/api/admin/notifications")
    public List<AdminNotificationResponse> notifications(Authentication authentication,
            @RequestParam(required = false) Integer limit) {
        return systemConfigService.notifications(CurrentUser.require(authentication).userId(), limit);
    }

    @PostMapping("/api/admin/notifications/{id}/read")
    public AdminNotificationResponse markNotificationRead(Authentication authentication, @PathVariable Long id) {
        return systemConfigService.markNotificationRead(CurrentUser.require(authentication).userId(), id);
    }

    @PostMapping("/api/admin/notifications/bulk-read")
    public List<AdminNotificationResponse> bulkReadNotifications(Authentication authentication,
            @Valid @RequestBody BulkReadNotificationsRequest request) {
        return systemConfigService.bulkReadNotifications(CurrentUser.require(authentication).userId(), request.ids());
    }

    @GetMapping("/api/admin/notifications/settings")
    public SystemSettingResponse notificationSettings() {
        return systemConfigService.notificationSettings();
    }

    @PutMapping("/api/admin/notifications/settings")
    public SystemSettingResponse updateNotificationSettings(Authentication authentication,
            @Valid @RequestBody SystemSettingUpdateRequest request) {
        return systemConfigService.updateNotificationSettings(CurrentUser.require(authentication).userId(),
                request.value());
    }

    @GetMapping("/api/admin/health")
    public Map<String, Object> health() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "UP");
        response.put("timestamp", java.time.Instant.now().toString());
        response.put("service", "KendyDigital");
        return response;
    }
}
