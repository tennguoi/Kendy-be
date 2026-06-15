package com.example.KendyDigital.controller;

import com.example.KendyDigital.dto.catalog.request.CreateServiceCategoryRequest;
import com.example.KendyDigital.dto.catalog.request.UpdateServiceCategoryRequest;
import com.example.KendyDigital.dto.catalog.response.ServiceCategoryResponse;
import com.example.KendyDigital.security.CurrentUser;
import com.example.KendyDigital.service.catalog.ServiceCategoryService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ServiceCategoryController {
    private final ServiceCategoryService serviceCategoryService;

    public ServiceCategoryController(ServiceCategoryService serviceCategoryService) {
        this.serviceCategoryService = serviceCategoryService;
    }

    @GetMapping("/api/admin/service-categories")
    public List<ServiceCategoryResponse> listCategories() {
        return serviceCategoryService.listAll();
    }

    @GetMapping("/api/service-categories")
    public List<ServiceCategoryResponse> listPublicCategories() {
        return serviceCategoryService.listAll();
    }

    @GetMapping("/api/service-categories/root")
    public List<ServiceCategoryResponse> listPublicRootCategories() {
        return serviceCategoryService.listRoot();
    }

    @GetMapping("/api/service-categories/{id}")
    public ServiceCategoryResponse getPublicCategory(@PathVariable Long id) {
        return serviceCategoryService.getById(id);
    }

    @GetMapping("/api/service-categories/slug/{slug}")
    public ServiceCategoryResponse getPublicCategoryBySlug(@PathVariable String slug) {
        return serviceCategoryService.getBySlug(slug);
    }

    @GetMapping("/api/admin/service-categories/{id}")
    public ServiceCategoryResponse getCategory(@PathVariable Long id) {
        return serviceCategoryService.getById(id);
    }

    @PostMapping("/api/admin/service-categories")
    public ServiceCategoryResponse createCategory(Authentication authentication,
            @Valid @RequestBody CreateServiceCategoryRequest request) {
        return serviceCategoryService.create(CurrentUser.require(authentication).userId(), request);
    }

    @PutMapping("/api/admin/service-categories/{id}")
    public ServiceCategoryResponse updateCategory(Authentication authentication, @PathVariable Long id,
            @Valid @RequestBody UpdateServiceCategoryRequest request) {
        return serviceCategoryService.update(CurrentUser.require(authentication).userId(), id, request);
    }

    @DeleteMapping("/api/admin/service-categories/{id}")
    public void deleteCategory(Authentication authentication, @PathVariable Long id) {
        serviceCategoryService.delete(CurrentUser.require(authentication).userId(), id);
    }
}
