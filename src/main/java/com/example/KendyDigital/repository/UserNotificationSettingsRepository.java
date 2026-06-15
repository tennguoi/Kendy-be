package com.example.KendyDigital.repository;

import com.example.KendyDigital.model.user.UserNotificationSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserNotificationSettingsRepository extends JpaRepository<UserNotificationSettings, Long> {
}
