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

public interface ServiceCatalogService {
    ServiceResponse create(Long adminUserId, CreateServiceRequest request);
    List<ServiceResponse> listActive();
    List<ServiceResponse> searchActive(String query, Long categoryId, Integer limit);
    List<ServiceResponse> searchActive(String query, Long categoryId, String categorySlug, String sort, Integer limit);
    List<ServiceResponse> listForAdmin(Integer limit);
    List<ServiceResponse> searchForAdmin(String query, ServiceStatus status, Integer limit);
    List<ServiceResponse> searchForAdmin(String query, ServiceStatus status, Long categoryId, String categorySlug, Boolean featured, String sort, Integer limit);
    List<ServicePricingResponse> searchPricing(String query, Long categoryId, String categorySlug, Boolean featured, String sort, Integer limit);
    List<ServicePricingResponse> searchPricingForAdmin(String query, ServiceStatus status, Long categoryId, String categorySlug, Boolean featured, String sort, Integer limit);
    ServiceResponse getBySlug(String slug);
    ServiceResponse update(Long id, Long adminUserId, UpdateServiceRequest request);
    ServiceResponse updateStatus(Long id, Long adminUserId, ServiceStatusUpdateRequest request);
    ServiceResponse delete(Long id, Long adminUserId);
    List<com.example.KendyDigital.dto.catalog.response.ServiceCategoryResponse> getCategories(Long id);
    List<OrderResponse> listOrders(Long id, Integer limit);
    List<ServiceResponse> bulkStatus(Long adminUserId, List<Long> ids, ServiceStatus status, String reason);
}
