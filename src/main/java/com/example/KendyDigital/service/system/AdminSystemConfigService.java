package com.example.KendyDigital.service.system;

import com.example.KendyDigital.dto.notification.response.AdminNotificationResponse;
import com.example.KendyDigital.dto.setting.request.SystemSettingsBulkUpdateRequest;
import com.example.KendyDigital.dto.setting.request.WebhookConfigRequest;
import com.example.KendyDigital.dto.setting.response.SystemSettingHistoryResponse;
import com.example.KendyDigital.dto.setting.response.SystemSettingResponse;
import java.util.List;
import java.util.Map;

public interface AdminSystemConfigService {
    List<SystemSettingResponse> searchSettings(String query, Integer limit);
    List<SystemSettingHistoryResponse> settingHistory(String key, Integer limit);
    List<SystemSettingResponse> bulkUpdateSettings(Long adminUserId, SystemSettingsBulkUpdateRequest request);
    List<SystemSettingResponse> backupSettings();
    List<SystemSettingResponse> restoreSettings(Long adminUserId, SystemSettingsBulkUpdateRequest request);
    Map<String, Object> sepayStatus();
    List<SystemSettingResponse> getSePayConfig();
    List<SystemSettingResponse> updateSePayConfig(Long adminUserId, WebhookConfigRequest request);
    List<AdminNotificationResponse> notifications(Long adminUserId, Integer limit);
    AdminNotificationResponse markNotificationRead(Long adminUserId, Long id);
    List<AdminNotificationResponse> bulkReadNotifications(Long adminUserId, List<Long> ids);
    SystemSettingResponse notificationSettings();
    SystemSettingResponse updateNotificationSettings(Long adminUserId, String value);
}
