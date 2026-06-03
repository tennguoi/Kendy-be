package com.example.KendyDigital.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.KendyDigital.model.UserNotificationSettings;

public interface UserNotificationSettingsRepository extends JpaRepository<UserNotificationSettings, Long> {
}
