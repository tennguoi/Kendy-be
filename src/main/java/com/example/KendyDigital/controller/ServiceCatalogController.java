package com.example.KendyDigital.controller;

import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.KendyDigital.dto.CreateServiceRequest;
import com.example.KendyDigital.dto.IdsRequest;
import com.example.KendyDigital.dto.OrderResponse;
import com.example.KendyDigital.dto.ServiceResponse;
import com.example.KendyDigital.dto.ServiceStatusUpdateRequest;
import com.example.KendyDigital.dto.UpdateServiceRequest;
import com.example.KendyDigital.model.ServiceStatus;
import com.example.KendyDigital.security.CurrentUser;
import com.example.KendyDigital.service.ServiceCatalogService;

import jakarta.validation.Valid;

@RestController
public class ServiceCatalogController {
    private final ServiceCatalogService serviceCatalogService;

    public ServiceCatalogController(ServiceCatalogService serviceCatalogService) {
        this.serviceCatalogService = serviceCatalogService;
    }

    @GetMapping("/api/services")
    public List<ServiceResponse> listActive() {
        return serviceCatalogService.listActive();
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
            @RequestParam(required = false) Integer limit) {
        return serviceCatalogService.searchForAdmin(query, status, limit);
    }

    @GetMapping("/api/admin/services/{id}/categories")
    public List<Map<String, Object>> getCategories(@PathVariable Long id) {
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
