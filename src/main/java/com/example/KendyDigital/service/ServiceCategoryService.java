package com.example.KendyDigital.service;

import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.KendyDigital.dto.CreateServiceCategoryRequest;
import com.example.KendyDigital.dto.ServiceCategoryResponse;
import com.example.KendyDigital.dto.UpdateServiceCategoryRequest;
import com.example.KendyDigital.model.ServiceCategory;
import com.example.KendyDigital.repository.ServiceCategoryRepository;

@Service
public class ServiceCategoryService {
    private final ServiceCategoryRepository serviceCategoryRepository;
    private final AuditService auditService;

    public ServiceCategoryService(ServiceCategoryRepository serviceCategoryRepository, AuditService auditService) {
        this.serviceCategoryRepository = serviceCategoryRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<ServiceCategoryResponse> listAll() {
        return serviceCategoryRepository.findAllByOrderBySortOrderAscNameAsc()
                .stream()
                .map(ServiceCategoryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ServiceCategoryResponse> listRoot() {
        return serviceCategoryRepository.findAllByParentIsNullOrderBySortOrderAscNameAsc()
                .stream()
                .map(ServiceCategoryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ServiceCategoryResponse getById(Long id) {
        return ServiceCategoryResponse.from(requireCategory(id));
    }

    @Transactional(readOnly = true)
    public ServiceCategoryResponse getBySlug(String slug) {
        return serviceCategoryRepository.findBySlug(normalizeSlug(slug))
                .map(ServiceCategoryResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
    }

    @Transactional
    public ServiceCategoryResponse create(Long adminUserId, CreateServiceCategoryRequest request) {
        String slug = normalizeSlug(request.slug());
        if (serviceCategoryRepository.existsBySlug(slug)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Category slug already exists");
        }

        ServiceCategory parent = null;
        if (request.parentId() != null) {
            parent = requireCategory(request.parentId());
        }

        ServiceCategory category = new ServiceCategory(
                request.name().trim(),
                slug,
                request.description(),
                request.sortOrder() == null ? 0 : request.sortOrder());
        if (parent != null) {
            category.setParent(parent);
        }

        ServiceCategory saved = serviceCategoryRepository.save(category);
        auditService.recordAdmin(adminUserId, "SERVICE_CATEGORY_CREATED", "SERVICE_CATEGORY", saved.getId(),
                "slug=" + saved.getSlug());
        return ServiceCategoryResponse.from(saved);
    }

    @Transactional
    public ServiceCategoryResponse update(Long adminUserId, Long id, UpdateServiceCategoryRequest request) {
        ServiceCategory category = requireCategory(id);

        if (request.name() != null) {
            category.setName(request.name().trim());
        }
        if (request.slug() != null) {
            String slug = normalizeSlug(request.slug());
            if (!slug.equals(category.getSlug()) && serviceCategoryRepository.existsBySlug(slug)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Category slug already exists");
            }
            category.setSlug(slug);
        }
        if (request.description() != null) {
            category.setDescription(request.description());
        }
        if (request.sortOrder() != null) {
            category.setSortOrder(request.sortOrder());
        }
        if (request.parentId() != null) {
            if (request.parentId().equals(category.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category cannot be its own parent");
            }
            category.setParent(requireCategory(request.parentId()));
        } else if (request.parentId() == null && category.getParent() != null) {
            category.setParent(null);
        }

        auditService.recordAdmin(adminUserId, "SERVICE_CATEGORY_UPDATED", "SERVICE_CATEGORY", category.getId(),
                "slug=" + category.getSlug());
        return ServiceCategoryResponse.from(category);
    }

    @Transactional
    public void delete(Long adminUserId, Long id) {
        ServiceCategory category = requireCategory(id);
        serviceCategoryRepository.delete(category);
        auditService.recordAdmin(adminUserId, "SERVICE_CATEGORY_DELETED", "SERVICE_CATEGORY", id,
                "slug=" + category.getSlug());
    }

    private ServiceCategory requireCategory(Long id) {
        return serviceCategoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
    }

    private String normalizeSlug(String slug) {
        if (slug == null || slug.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Slug is required");
        }
        return slug.trim().toLowerCase(Locale.ROOT);
    }
}
