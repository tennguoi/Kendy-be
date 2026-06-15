package com.example.KendyDigital.controller;

import com.example.KendyDigital.dto.catalog.request.CreateServiceRequest;
import com.example.KendyDigital.dto.catalog.request.IdsRequest;
import com.example.KendyDigital.dto.catalog.request.ServiceStatusUpdateRequest;
import com.example.KendyDigital.dto.catalog.request.UpdateServiceRequest;
import com.example.KendyDigital.dto.catalog.response.ServiceCategoryResponse;
import com.example.KendyDigital.dto.catalog.response.ServicePricingResponse;
import com.example.KendyDigital.dto.catalog.response.ServiceResponse;
import com.example.KendyDigital.dto.order.response.OrderResponse;
import com.example.KendyDigital.model.catalog.ServiceStatus;
import com.example.KendyDigital.security.CurrentUser;
import com.example.KendyDigital.service.catalog.ServiceCatalogService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ServiceCatalogController {
    private final ServiceCatalogService serviceCatalogService;

    public ServiceCatalogController(ServiceCatalogService serviceCatalogService) {
        this.serviceCatalogService = serviceCatalogService;
    }

    @GetMapping("/api/services")
    public List<ServiceResponse> listActive(@RequestParam(required = false) String query,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String categorySlug,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) Integer limit) {
        return serviceCatalogService.searchActive(query, categoryId, categorySlug, sort, limit);
    }

    @GetMapping("/api/services/search")
    public List<ServiceResponse> searchActive(@RequestParam(required = false) String query,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String categorySlug,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) Integer limit) {
        return serviceCatalogService.searchActive(query, categoryId, categorySlug, sort, limit);
    }

    @GetMapping("/api/pricing")
    public List<ServicePricingResponse> listPricing(@RequestParam(required = false) String query,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String categorySlug,
            @RequestParam(required = false) Boolean featured,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) Integer limit) {
        return serviceCatalogService.searchPricing(query, categoryId, categorySlug, featured, sort, limit);
    }

    @GetMapping("/api/pricing/search")
    public List<ServicePricingResponse> searchPricing(@RequestParam(required = false) String query,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String categorySlug,
            @RequestParam(required = false) Boolean featured,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) Integer limit) {
        return serviceCatalogService.searchPricing(query, categoryId, categorySlug, featured, sort, limit);
    }

    @GetMapping("/api/services/{slug}")
    public ServiceResponse getBySlug(@PathVariable String slug) {
        return serviceCatalogService.getBySlug(slug);
    }

    @GetMapping("/api/admin/services")
    public List<ServiceResponse> listForAdmin() {
        return serviceCatalogService.listForAdmin();
    }

    @GetMapping("/api/admin/services/search")
    public List<ServiceResponse> searchForAdmin(@RequestParam(required = false) String query,
            @RequestParam(required = false) ServiceStatus status,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String categorySlug,
            @RequestParam(required = false) Boolean featured,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) Integer limit) {
        return serviceCatalogService.searchForAdmin(query, status, categoryId, categorySlug, featured, sort, limit);
    }

    @GetMapping("/api/admin/pricing")
    public List<ServicePricingResponse> pricingForAdmin(@RequestParam(required = false) String query,
            @RequestParam(required = false) ServiceStatus status,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String categorySlug,
            @RequestParam(required = false) Boolean featured,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) Integer limit) {
        return serviceCatalogService.searchPricingForAdmin(query, status, categoryId, categorySlug, featured, sort,
                limit);
    }

    @GetMapping("/api/admin/pricing/search")
    public List<ServicePricingResponse> searchPricingForAdmin(@RequestParam(required = false) String query,
            @RequestParam(required = false) ServiceStatus status,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String categorySlug,
            @RequestParam(required = false) Boolean featured,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) Integer limit) {
        return serviceCatalogService.searchPricingForAdmin(query, status, categoryId, categorySlug, featured, sort,
                limit);
    }

    @GetMapping("/api/admin/services/{id}/categories")
    public List<ServiceCategoryResponse> getCategories(@PathVariable Long id) {
        return serviceCatalogService.getCategories(id);
    }

    @GetMapping("/api/admin/services/{id}/orders")
    public List<OrderResponse> listOrders(@PathVariable Long id,
            @RequestParam(required = false) Integer limit) {
        return serviceCatalogService.listOrders(id, limit);
    }

    @PostMapping("/api/admin/services")
    public ServiceResponse create(Authentication authentication, @Valid @RequestBody CreateServiceRequest request) {
        return serviceCatalogService.create(CurrentUser.require(authentication).userId(), request);
    }

    @PutMapping("/api/admin/services/{id}")
    public ServiceResponse update(Authentication authentication, @PathVariable Long id,
            @Valid @RequestBody UpdateServiceRequest request) {
        return serviceCatalogService.update(id, CurrentUser.require(authentication).userId(), request);
    }

    @PatchMapping("/api/admin/services/{id}/status")
    public ServiceResponse updateStatus(Authentication authentication, @PathVariable Long id,
            @Valid @RequestBody ServiceStatusUpdateRequest request) {
        return serviceCatalogService.updateStatus(id, CurrentUser.require(authentication).userId(), request);
    }

    @DeleteMapping("/api/admin/services/{id}")
    public ServiceResponse delete(Authentication authentication, @PathVariable Long id) {
        return serviceCatalogService.delete(id, CurrentUser.require(authentication).userId());
    }

    @PostMapping("/api/admin/services/bulk-enable")
    public List<ServiceResponse> bulkEnable(Authentication authentication, @Valid @RequestBody IdsRequest request) {
        return serviceCatalogService.bulkStatus(CurrentUser.require(authentication).userId(), request.ids(),
                ServiceStatus.ACTIVE, request.reason());
    }

    @PostMapping("/api/admin/services/bulk-disable")
    public List<ServiceResponse> bulkDisable(Authentication authentication, @Valid @RequestBody IdsRequest request) {
        return serviceCatalogService.bulkStatus(CurrentUser.require(authentication).userId(), request.ids(),
                ServiceStatus.INACTIVE, request.reason());
    }
}
