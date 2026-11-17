package com.example.KendyDigital.service.user_management;

import com.example.KendyDigital.dto.catalog.response.ServiceResponse;
import java.util.List;
import java.util.UUID;

public interface UserServicePreferenceService {
    List<ServiceResponse> favorites(Long userId, int page, int size);
    ServiceResponse addFavorite(Long userId, UUID serviceId);
    void removeFavorite(Long userId, UUID serviceId);
    List<ServiceResponse> recent(Long userId, int page, int size);
}
