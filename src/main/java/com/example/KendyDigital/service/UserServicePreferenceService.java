package com.example.KendyDigital.service;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.KendyDigital.dto.catalog.response.ServiceResponse;
import com.example.KendyDigital.model.ServiceItem;
import com.example.KendyDigital.model.ServiceStatus;
import com.example.KendyDigital.model.UserAccount;
import com.example.KendyDigital.model.UserFavoriteService;
import com.example.KendyDigital.repository.ServiceItemRepository;
import com.example.KendyDigital.repository.UserAccountRepository;
import com.example.KendyDigital.repository.UserFavoriteServiceRepository;

@Service
public class UserServicePreferenceService {
    private final UserFavoriteServiceRepository favoriteRepository;
    private final UserAccountRepository userAccountRepository;
    private final ServiceItemRepository serviceItemRepository;

    public UserServicePreferenceService(UserFavoriteServiceRepository favoriteRepository,
            UserAccountRepository userAccountRepository,
            ServiceItemRepository serviceItemRepository) {
        this.favoriteRepository = favoriteRepository;
        this.userAccountRepository = userAccountRepository;
        this.serviceItemRepository = serviceItemRepository;
    }

    @Transactional(readOnly = true)
    public List<ServiceResponse> favorites(Long userId, int page, int size) {
        return favoriteRepository.findAllByUser_IdOrderByCreatedAtDesc(userId, paged(page, size))
                .stream()
                .map(UserFavoriteService::getService)
                .filter(service -> service.getStatus() == ServiceStatus.ACTIVE)
                .map(ServiceResponse::from)
                .toList();
    }

    @Transactional
    public ServiceResponse addFavorite(Long userId, Long serviceId) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        ServiceItem service = serviceItemRepository.findById(serviceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service not found"));
        if (service.getStatus() != ServiceStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Service not found");
        }
        return favoriteRepository.findByUser_IdAndService_Id(userId, serviceId)
                .map(UserFavoriteService::getService)
                .map(ServiceResponse::from)
                .orElseGet(() -> ServiceResponse.from(
                        favoriteRepository.save(new UserFavoriteService(user, service)).getService()));
    }

    @Transactional
    public void removeFavorite(Long userId, Long serviceId) {
        favoriteRepository.findByUser_IdAndService_Id(userId, serviceId)
                .ifPresent(favoriteRepository::delete);
    }

    @Transactional(readOnly = true)
    public List<ServiceResponse> recent(Long userId, int page, int size) {
        return favoriteRepository.findRecentServices(userId, paged(page, size))
                .stream()
                .filter(service -> service.getStatus() == ServiceStatus.ACTIVE)
                .map(ServiceResponse::from)
                .toList();
    }

    private PageRequest paged(int page, int size) {
        return PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 100)));
    }
}
