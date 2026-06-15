package com.example.KendyDigital.service.user;

import com.example.KendyDigital.dto.catalog.response.ServiceResponse;
import java.util.List;

public interface UserServicePreferenceService {
    List<ServiceResponse> favorites(Long userId, int page, int size);
    ServiceResponse addFavorite(Long userId, Long serviceId);
    void removeFavorite(Long userId, Long serviceId);
    List<ServiceResponse> recent(Long userId, int page, int size);
}
