package com.example.KendyDigital.service.catalog;

import com.example.KendyDigital.dto.catalog.request.CreateServiceRequest;
import com.example.KendyDigital.dto.catalog.request.ServiceStatusUpdateRequest;
import com.example.KendyDigital.dto.catalog.request.UpdateServiceRequest;
import com.example.KendyDigital.dto.catalog.response.ServiceCategoryResponse;
import com.example.KendyDigital.dto.catalog.response.ServicePricingResponse;
import com.example.KendyDigital.dto.catalog.response.ServiceResponse;
import com.example.KendyDigital.dto.order.response.OrderResponse;
import com.example.KendyDigital.model.catalog.ServiceStatus;
import java.util.List;
import java.util.UUID;

public interface ServiceCatalogService {
    ServiceResponse create(Long adminUserId, CreateServiceRequest request);
    List<ServiceResponse> listActive();
    List<ServiceResponse> searchActive(String query, UUID categoryId, Integer limit);
    List<ServiceResponse> searchActive(String query, UUID categoryId, String categorySlug, String sort, Integer limit);
    List<ServiceResponse> listForAdmin(Integer limit);
    List<ServiceResponse> searchForAdmin(String query, ServiceStatus status, Integer limit);
    List<ServiceResponse> searchForAdmin(String query, ServiceStatus status, UUID categoryId, String categorySlug, Boolean featured, String sort, Integer limit);
    List<ServicePricingResponse> searchPricing(String query, UUID categoryId, String categorySlug, Boolean featured, String sort, Integer limit);
    List<ServicePricingResponse> searchPricingForAdmin(String query, ServiceStatus status, UUID categoryId, String categorySlug, Boolean featured, String sort, Integer limit);
    ServiceResponse getBySlug(String slug);
    ServiceResponse update(UUID publicId, Long adminUserId, UpdateServiceRequest request);
    ServiceResponse updateStatus(UUID publicId, Long adminUserId, ServiceStatusUpdateRequest request);
    ServiceResponse delete(UUID publicId, Long adminUserId);
    List<com.example.KendyDigital.dto.catalog.response.ServiceCategoryResponse> getCategories(UUID publicId);
    List<OrderResponse> listOrders(UUID publicId, Integer limit);
    List<ServiceResponse> bulkStatus(Long adminUserId, List<UUID> ids, ServiceStatus status, String reason);
}
