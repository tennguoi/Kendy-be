package com.example.KendyDigital.service.catalog;

import com.example.KendyDigital.dto.catalog.request.CreateServiceRequest;
import com.example.KendyDigital.dto.catalog.request.ServiceStatusUpdateRequest;
import com.example.KendyDigital.dto.catalog.request.UpdateServiceRequest;
import com.example.KendyDigital.dto.catalog.response.ServiceCategoryResponse;
import com.example.KendyDigital.dto.catalog.response.ServicePricingResponse;
import com.example.KendyDigital.dto.catalog.response.ServiceResponse;
import com.example.KendyDigital.dto.order.response.OrderResponse;
import com.example.KendyDigital.model.catalog.ServiceCategory;
import com.example.KendyDigital.model.catalog.ServiceCtaType;
import com.example.KendyDigital.model.catalog.ServiceItem;
import com.example.KendyDigital.model.catalog.ServiceStatus;
import com.example.KendyDigital.model.catalog.ServiceStockStatus;
import com.example.KendyDigital.model.catalog.ServiceType;
import com.example.KendyDigital.repository.OrderRepository;
import com.example.KendyDigital.repository.ServiceCategoryRepository;
import com.example.KendyDigital.repository.ServiceItemRepository;
import com.example.KendyDigital.service.audit.AuditService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ServiceCatalogServiceImpl  implements ServiceCatalogService{
    private final ServiceItemRepository serviceItemRepository;
    private final OrderRepository orderRepository;
    private final AuditService auditService;
    private final ServiceCategoryRepository serviceCategoryRepository;

    public ServiceCatalogServiceImpl(ServiceItemRepository serviceItemRepository, OrderRepository orderRepository,
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
        if (request.priceText() != null) {
            service.updatePriceText(blankToNull(request.priceText()));
        }
        if (request.costPrice() != null) {
            service.updateCostPrice(normalizeMoney(request.costPrice()));
        }
        service.updatePricingMetadata(
                request.stockStatus() == null ? ServiceStockStatus.AVAILABLE : request.stockStatus(),
                request.ctaType() == null ? ServiceCtaType.BUY_NOW : request.ctaType(),
                blankToNull(request.pricingBadge()),
                Boolean.TRUE.equals(request.featured()),
                request.publicVisible() == null || request.publicVisible());
        if (request.inputSchema() != null) {
            service.updateInputSchema(request.inputSchema());
        }
        service.updatePublicContent(
                blankToNull(request.requirements()),
                blankToNull(request.benefits()),
                blankToNull(request.usageNotes()));
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
        return searchActive(query, categoryId, null, null, limit);
    }

    @Transactional(readOnly = true)
    public List<ServiceResponse> searchActive(String query, Long categoryId, String categorySlug, String sort,
            Integer limit) {
        String normalizedQuery = query == null || query.isBlank() ? null : query.trim();
        String queryPattern = likePattern(normalizedQuery);
        int normalizedLimit = normalizedLimit(limit);
        List<ServiceItem> services = serviceItemRepository.searchPublic(
                queryPattern,
                parseLongOrNull(normalizedQuery),
                categoryId,
                normalizeOptionalSlug(categorySlug),
                null,
                page(200));
        java.util.Map<Long, Long> orderCounts = new java.util.HashMap<>();
        if ("popular".equalsIgnoreCase(sort) || "most_purchased".equalsIgnoreCase(sort)) {
            orderCounts = getServiceOrderCounts();
        }
        return services
                .stream()
                .sorted(comparator(sort, orderCounts))
                .limit(normalizedLimit)
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
        return searchForAdmin(query, status, null, null, null, null, limit);
    }

    @Transactional(readOnly = true)
    public List<ServiceResponse> searchForAdmin(String query, ServiceStatus status, Long categoryId,
            String categorySlug, Boolean featured, String sort, Integer limit) {
        String normalizedQuery = query == null || query.isBlank() ? null : query.trim();
        String queryPattern = likePattern(normalizedQuery);
        int normalizedLimit = normalizedLimit(limit);
        List<ServiceItem> services = serviceItemRepository.searchAdmin(
                queryPattern,
                parseLongOrNull(normalizedQuery),
                status,
                categoryId,
                normalizeOptionalSlug(categorySlug),
                featured,
                page(200));
        return services
                .stream()
                .sorted(comparator(sort))
                .limit(normalizedLimit)
                .map(ServiceResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ServicePricingResponse> searchPricing(String query, Long categoryId, String categorySlug,
            Boolean featured, String sort, Integer limit) {
        String normalizedQuery = query == null || query.isBlank() ? null : query.trim();
        String queryPattern = likePattern(normalizedQuery);
        int normalizedLimit = normalizedLimit(limit);
        java.util.Map<Long, Long> orderCounts = new java.util.HashMap<>();
        if ("popular".equalsIgnoreCase(sort) || "most_purchased".equalsIgnoreCase(sort)) {
            orderCounts = getServiceOrderCounts();
        }
        return serviceItemRepository.searchPublic(
                queryPattern,
                parseLongOrNull(normalizedQuery),
                categoryId,
                normalizeOptionalSlug(categorySlug),
                featured,
                page(200))
                .stream()
                .sorted(comparator(sort, orderCounts))
                .limit(normalizedLimit)
                .map(ServicePricingResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ServicePricingResponse> searchPricingForAdmin(String query, ServiceStatus status, Long categoryId,
            String categorySlug, Boolean featured, String sort, Integer limit) {
        String normalizedQuery = query == null || query.isBlank() ? null : query.trim();
        String queryPattern = likePattern(normalizedQuery);
        int normalizedLimit = normalizedLimit(limit);
        return serviceItemRepository.searchAdmin(
                queryPattern,
                parseLongOrNull(normalizedQuery),
                status,
                categoryId,
                normalizeOptionalSlug(categorySlug),
                featured,
                page(200))
                .stream()
                .sorted(comparator(sort))
                .limit(normalizedLimit)
                .map(ServicePricingResponse::from)
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
        if (request.priceText() != null) {
            service.updatePriceText(blankToNull(request.priceText()));
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
        if (request.stockStatus() != null || request.ctaType() != null || request.pricingBadge() != null
                || request.featured() != null || request.publicVisible() != null) {
            service.updatePricingMetadata(
                    request.stockStatus() == null ? service.getStockStatus() : request.stockStatus(),
                    request.ctaType() == null ? service.getCtaType() : request.ctaType(),
                    request.pricingBadge() == null ? service.getPricingBadge() : blankToNull(request.pricingBadge()),
                    request.featured() == null ? service.isFeatured() : request.featured(),
                    request.publicVisible() == null ? service.isPublicVisible() : request.publicVisible());
        }
        if (request.inputSchema() != null) {
            service.updateInputSchema(request.inputSchema());
        }
        if (request.requirements() != null || request.benefits() != null || request.usageNotes() != null) {
            service.updatePublicContent(
                    request.requirements() == null ? service.getRequirements() : blankToNull(request.requirements()),
                    request.benefits() == null ? service.getBenefits() : blankToNull(request.benefits()),
                    request.usageNotes() == null ? service.getUsageNotes() : blankToNull(request.usageNotes()));
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
        } else if (Boolean.TRUE.equals(request.clearCategory()) && service.getCategory() != null) {
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
    public List<com.example.KendyDigital.dto.catalog.response.ServiceCategoryResponse> getCategories(Long id) {
        ServiceItem service = serviceItemRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service not found"));
        if (service.getCategory() == null) {
            return List.of();
        }
        return List.of(com.example.KendyDigital.dto.catalog.response.ServiceCategoryResponse.from(service.getCategory()));
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
        List<ServiceItem> services = serviceItemRepository.findAllById(ids);
        if (services.size() != ids.size()) {
            List<Long> found = services.stream().map(ServiceItem::getId).toList();
            Long missing = ids.stream().filter(id -> !found.contains(id)).findFirst().orElse(null);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Service not found: " + missing);
        }
        return services.stream()
                .map(service -> {
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
        return PageRequest.of(0, normalizedLimit(limit));
    }

    private int normalizedLimit(Integer limit) {
        return limit == null ? 100 : Math.max(1, Math.min(limit, 200));
    }

    private String normalizeOptionalSlug(String slug) {
        return slug == null || slug.isBlank() ? null : slug.trim().toLowerCase(Locale.ROOT);
    }

    private String likePattern(String query) {
        return query == null ? null : "%" + query.toLowerCase(Locale.ROOT) + "%";
    }

    private java.util.Map<Long, Long> getServiceOrderCounts() {
        java.util.Map<Long, Long> counts = new java.util.HashMap<>();
        List<Object[]> performance = orderRepository.servicePerformance(PageRequest.of(0, 1000));
        for (Object[] row : performance) {
            if (row.length >= 3 && row[0] instanceof Long serviceId && row[2] instanceof Long count) {
                counts.put(serviceId, count);
            }
        }
        return counts;
    }

    private Comparator<ServiceItem> comparator(String sort) {
        return comparator(sort, java.util.Collections.emptyMap());
    }

    private Comparator<ServiceItem> comparator(String sort, java.util.Map<Long, Long> orderCounts) {
        String normalized = sort == null || sort.isBlank()
                ? "sort_order"
                : sort.trim().toLowerCase(Locale.ROOT).replace("-", "_");
        Comparator<ServiceItem> defaultComparator = Comparator
                .comparingInt(ServiceItem::getSortOrder)
                .thenComparing(ServiceItem::getName, String.CASE_INSENSITIVE_ORDER);

        return switch (normalized) {
            case "name", "name_asc" -> Comparator.comparing(ServiceItem::getName, String.CASE_INSENSITIVE_ORDER);
            case "price", "price_asc" -> Comparator.comparing(ServiceItem::getPrice).thenComparing(defaultComparator);
            case "price_desc" -> Comparator.comparing(ServiceItem::getPrice).reversed().thenComparing(defaultComparator);
            case "newest", "created_desc" -> Comparator.comparing(ServiceItem::getCreatedAt).reversed();
            case "featured" -> Comparator.comparing(ServiceItem::isFeatured).reversed().thenComparing(defaultComparator);
            case "popular", "most_purchased" -> {
                yield Comparator.<ServiceItem, Long>comparing(s -> orderCounts.getOrDefault(s.getId(), 0L))
                        .reversed()
                        .thenComparing(defaultComparator);
            }
            default -> defaultComparator;
        };
    }
}
