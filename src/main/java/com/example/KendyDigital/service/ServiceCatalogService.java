package com.example.KendyDigital.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.KendyDigital.dto.CreateServiceRequest;
import com.example.KendyDigital.dto.OrderResponse;
import com.example.KendyDigital.dto.ServiceResponse;
import com.example.KendyDigital.dto.ServiceStatusUpdateRequest;
import com.example.KendyDigital.dto.UpdateServiceRequest;
import com.example.KendyDigital.model.ServiceCategory;
import com.example.KendyDigital.model.ServiceItem;
import com.example.KendyDigital.model.ServiceStatus;
import com.example.KendyDigital.model.ServiceType;
import com.example.KendyDigital.repository.OrderRepository;
import com.example.KendyDigital.repository.ServiceCategoryRepository;
import com.example.KendyDigital.repository.ServiceItemRepository;

@Service
public class ServiceCatalogService {
    private final ServiceItemRepository serviceItemRepository;
    private final OrderRepository orderRepository;
    private final AuditService auditService;
    private final ServiceCategoryRepository serviceCategoryRepository;

    public ServiceCatalogService(ServiceItemRepository serviceItemRepository, OrderRepository orderRepository,
            AuditService auditService, ServiceCategoryRepository serviceCategoryRepository) {
        this.serviceItemRepository = serviceItemRepository;
        this.orderRepository = orderRepository;
        this.auditService = auditService;
        this.serviceCategoryRepository = serviceCategoryRepository;
    }

    @Transactional
    public ServiceResponse create(Long adminUserId, CreateServiceRequest request) {
        String slug = normalizeSlug(request.slug());
        if (serviceItemRepository.existsBySlug(slug)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Service slug already exists");
        }

        ServiceItem service = new ServiceItem(
                requireTrimmed(request.name(), "Service name is required"),
                slug,
                request.shortDescription(),
                request.description(),
                normalizeMoney(request.price()),
                request.type() == null ? ServiceType.MANUAL : request.type(),
                request.status() == null ? ServiceStatus.ACTIVE : request.status());
        if (request.costPrice() != null) {
            service.updateCostPrice(normalizeMoney(request.costPrice()));
        }
        if (request.inputSchema() != null) {
            service.updateInputSchema(request.inputSchema());
        }
        if (request.processingTime() != null) {
            service.updateProcessingTime(request.processingTime());
        }
        if (request.warrantyPolicy() != null) {
            service.updateWarrantyPolicy(request.warrantyPolicy());
        }
        if (request.sortOrder() != null) {
            service.updateSortOrder(request.sortOrder());
        }
        if (request.categoryId() != null) {
            ServiceCategory category = serviceCategoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
            service.updateCategory(category);
        }
        service.updateSeo(request.metaTitle(), request.metaDescription(), request.iconUrl());

        ServiceItem saved = serviceItemRepository.save(service);
        auditService.recordAdmin(adminUserId, "SERVICE_CREATED", "SERVICE", saved.getId(), "slug=" + saved.getSlug());
        return ServiceResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<ServiceResponse> listActive() {
        return searchActive(null, null, 100);
    }

    @Transactional(readOnly = true)
    public List<ServiceResponse> searchActive(String query, Long categoryId, Integer limit) {
        String normalizedQuery = query == null || query.isBlank() ? null : query.trim();
        PageRequest pageable = page(limit);
        List<ServiceItem> services = normalizedQuery == null
                ? listActiveWithoutSearch(categoryId, pageable)
                : serviceItemRepository.searchPublic(
                        normalizedQuery,
                        parseLongOrNull(normalizedQuery),
                        categoryId,
                        pageable);
        return services
                .stream()
                .map(ServiceResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ServiceResponse> listForAdmin() {
        return serviceItemRepository.findAllByOrderBySortOrderAscNameAsc()
                .stream()
                .map(ServiceResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ServiceResponse> searchForAdmin(String query, ServiceStatus status, Integer limit) {
        String normalizedQuery = query == null || query.isBlank() ? null : query.trim();
        PageRequest pageable = page(limit);
        List<ServiceItem> services = normalizedQuery == null
                ? listForAdminWithoutSearch(status, pageable)
                : serviceItemRepository.searchAdmin(
                        normalizedQuery,
                        parseLongOrNull(normalizedQuery),
                        status,
                        pageable);
        return services
                .stream()
                .map(ServiceResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ServiceResponse getBySlug(String slug) {
        ServiceItem service = serviceItemRepository.findBySlug(normalizeSlug(slug))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service not found"));
        if (service.getStatus() != ServiceStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Service not found");
        }
        return ServiceResponse.from(service);
    }

    @Transactional
    public ServiceResponse update(Long id, Long adminUserId, UpdateServiceRequest request) {
        ServiceItem service = serviceItemRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service not found"));

        if (request.name() != null || request.slug() != null) {
            String name = request.name() == null
                    ? service.getName()
                    : requireTrimmed(request.name(), "Service name is required");
            String slug = request.slug() == null ? service.getSlug() : normalizeSlug(request.slug());
            if (!slug.equals(service.getSlug()) && serviceItemRepository.existsBySlug(slug)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Service slug already exists");
            }
            service.rename(name, slug);
        }
        if (request.shortDescription() != null || request.description() != null) {
            service.updateDescriptions(
                    request.shortDescription() == null ? service.getShortDescription() : request.shortDescription(),
                    request.description() == null ? service.getDescription() : request.description());
        }
        if (request.price() != null) {
            service.updatePrice(normalizeMoney(request.price()));
        }
        if (request.costPrice() != null) {
            service.updateCostPrice(normalizeMoney(request.costPrice()));
        }
        if (request.type() != null) {
            service.updateType(request.type());
        }
        if (request.status() != null) {
            service.changeStatus(request.status());
        }
        if (request.inputSchema() != null) {
            service.updateInputSchema(request.inputSchema());
        }
        if (request.processingTime() != null) {
            service.updateProcessingTime(request.processingTime());
        }
        if (request.warrantyPolicy() != null) {
            service.updateWarrantyPolicy(request.warrantyPolicy());
        }
        if (request.sortOrder() != null) {
            service.updateSortOrder(request.sortOrder());
        }
        if (request.categoryId() != null) {
            ServiceCategory category = serviceCategoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
            service.updateCategory(category);
        } else if (service.getCategory() != null) {
            service.updateCategory(null);
        }
        if (request.metaTitle() != null || request.metaDescription() != null || request.iconUrl() != null) {
            service.updateSeo(
                    request.metaTitle() != null ? request.metaTitle() : service.getMetaTitle(),
                    request.metaDescription() != null ? request.metaDescription() : service.getMetaDescription(),
                    request.iconUrl() != null ? request.iconUrl() : service.getIconUrl());
        }

        auditService.recordAdmin(adminUserId, "SERVICE_UPDATED", "SERVICE", service.getId(), "slug=" + service.getSlug());
        return ServiceResponse.from(service);
    }

    @Transactional
    public ServiceResponse updateStatus(Long id, Long adminUserId, ServiceStatusUpdateRequest request) {
        ServiceItem service = serviceItemRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service not found"));
        service.changeStatus(request.status());
        auditService.recordAdmin(
                adminUserId,
                "SERVICE_STATUS_UPDATED",
                "SERVICE",
                service.getId(),
                "status=" + service.getStatus());
        return ServiceResponse.from(service);
    }

    @Transactional
    public ServiceResponse delete(Long id, Long adminUserId) {
        ServiceItem service = serviceItemRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service not found"));
        service.deactivate();
        auditService.recordAdmin(adminUserId, "SERVICE_DEACTIVATED", "SERVICE", service.getId(), "slug=" + service.getSlug());
        return ServiceResponse.from(service);
    }

    @Transactional(readOnly = true)
    public List<com.example.KendyDigital.dto.ServiceCategoryResponse> getCategories(Long id) {
        ServiceItem service = serviceItemRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service not found"));
        if (service.getCategory() == null) {
            return List.of();
        }
        return List.of(com.example.KendyDigital.dto.ServiceCategoryResponse.from(service.getCategory()));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> listOrders(Long id, Integer limit) {
        if (!serviceItemRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Service not found");
        }
        return orderRepository.findAllByService_IdOrderByCreatedAtDesc(id, page(limit))
                .stream()
                .map(OrderResponse::from)
                .toList();
    }

    @Transactional
    public List<ServiceResponse> bulkStatus(Long adminUserId, List<Long> ids, ServiceStatus status, String reason) {
        return ids.stream()
                .map(id -> {
                    ServiceItem service = serviceItemRepository.findById(id)
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                    "Service not found: " + id));
                    service.changeStatus(status);
                    auditService.recordAdmin(adminUserId, "SERVICE_BULK_STATUS_UPDATED", "SERVICE", service.getId(),
                            "status=" + status + ",reason=" + blankToNull(reason));
                    return ServiceResponse.from(service);
                })
                .toList();
    }

    private String normalizeSlug(String slug) {
        return requireTrimmed(slug, "Service slug is required").toLowerCase(Locale.ROOT);
    }

    private String requireTrimmed(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return value.trim();
    }

    private BigDecimal normalizeMoney(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private List<ServiceItem> listActiveWithoutSearch(Long categoryId, PageRequest pageable) {
        if (categoryId == null) {
            return serviceItemRepository.findByStatusOrderBySortOrderAscNameAsc(ServiceStatus.ACTIVE, pageable);
        }
        return serviceItemRepository.findByStatusAndCategory_IdOrderBySortOrderAscNameAsc(
                ServiceStatus.ACTIVE,
                categoryId,
                pageable);
    }

    private List<ServiceItem> listForAdminWithoutSearch(ServiceStatus status, PageRequest pageable) {
        if (status == null) {
            return serviceItemRepository.findAllByOrderBySortOrderAscNameAsc(pageable);
        }
        return serviceItemRepository.findByStatusOrderBySortOrderAscNameAsc(status, pageable);
    }

    private Long parseLongOrNull(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private PageRequest page(Integer limit) {
        int normalizedLimit = limit == null ? 100 : Math.max(1, Math.min(limit, 200));
        return PageRequest.of(0, normalizedLimit);
    }
}
