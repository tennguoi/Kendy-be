package com.example.KendyDigital.service.user_management;

import java.util.Map;

public interface UserDataPortabilityService {
    Map<String, Object> exportPersonalData(Long userId);
    Map<String, Object> deleteAccount(Long userId);
}
