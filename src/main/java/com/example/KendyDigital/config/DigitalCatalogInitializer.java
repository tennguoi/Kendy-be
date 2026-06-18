// package com.example.KendyDigital.config;

// import com.example.KendyDigital.model.catalog.ServiceCategory;
// import com.example.KendyDigital.model.catalog.ServiceCtaType;
// import com.example.KendyDigital.model.catalog.ServiceItem;
// import com.example.KendyDigital.model.catalog.ServiceStatus;
// import com.example.KendyDigital.model.catalog.ServiceStockStatus;
// import com.example.KendyDigital.model.catalog.ServiceType;
// import com.example.KendyDigital.repository.ServiceCategoryRepository;
// import com.example.KendyDigital.repository.ServiceItemRepository;
// import java.math.BigDecimal;
// import java.util.List;
// import java.util.Locale;
// import org.springframework.boot.CommandLineRunner;
// import org.springframework.stereotype.Component;
// import org.springframework.transaction.annotation.Transactional;

// @Component
// public class DigitalCatalogInitializer implements CommandLineRunner {
//     private final ServiceCategoryRepository serviceCategoryRepository;
//     private final ServiceItemRepository serviceItemRepository;
//     private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

//     public DigitalCatalogInitializer(ServiceCategoryRepository serviceCategoryRepository,
//             ServiceItemRepository serviceItemRepository,
//             org.springframework.jdbc.core.JdbcTemplate jdbcTemplate) {
//         this.serviceCategoryRepository = serviceCategoryRepository;
//         this.serviceItemRepository = serviceItemRepository;
//         this.jdbcTemplate = jdbcTemplate;
//     }

//     @Override
//     @Transactional
//     public void run(String... args) {
//         try {
//             jdbcTemplate.execute("ALTER TABLE services DROP CONSTRAINT IF EXISTS services_type_check");
//         } catch (Exception e) {
//             System.err.println("Failed to drop constraint: " + e.getMessage());
//         }
//         ServiceCategory ai = category("Tài khoản AI", "tai-khoan-ai",
//                 "ChatGPT, Claude, Gemini, Midjourney và các sản phẩm số giao tự động.", 10);
//         ServiceCategory design = category("Design & Productivity", "design-productivity",
//                 "Canva, CapCut và công cụ làm việc số.", 20);
//         ServiceCategory entertainment = category("Giải trí số", "giai-tri-so",
//                 "Netflix và các gói giải trí số hợp lệ.", 30);
//         ServiceCategory facebook = category("Dịch vụ Facebook", "dich-vu-facebook",
//                 "Tư vấn, setup và tối ưu quảng cáo xử lý thủ công.", 40);

//         accountStock("ChatGPT Plus", "chatgpt-plus", ai, "Tài khoản/gói ChatGPT giao tự động từ kho.", 299000, 10);
//         accountStock("Claude Pro", "claude-pro", ai, "Tài khoản/gói Claude giao tự động từ kho.", 299000, 20);
//         accountStock("Gemini Advanced", "gemini-advanced", ai, "Tài khoản/gói Gemini giao tự động từ kho.", 249000, 30);
//         accountStock("Midjourney", "midjourney", ai, "Tài khoản/gói Midjourney giao tự động từ kho.", 299000, 40);
//         accountStock("Canva Pro", "canva-pro", design, "Canva Pro giao tự động từ kho.", 99000, 50);
//         accountStock("CapCut Pro", "capcut-pro", design, "CapCut Pro giao tự động từ kho.", 129000, 60);
//         accountStock("Netflix", "netflix", entertainment, "Gói Netflix hợp lệ giao tự động từ kho.", 99000, 70);

//         manual("Tư vấn Facebook Ads", "tu-van-facebook-ads", facebook,
//                 "Tư vấn chiến dịch quảng cáo theo brief và mục tiêu kinh doanh.", 199000, 110);
//         manual("Setup chiến dịch Facebook", "setup-chien-dich-facebook", facebook,
//                 "Admin tiếp nhận brief, setup thủ công và cập nhật trạng thái trong hệ thống.", 499000, 120);
//         manual("Review quảng cáo Facebook", "review-quang-cao-facebook", facebook,
//                 "Review page, landing page, pixel và chiến dịch theo dữ liệu khách cung cấp.", 299000, 130);

//         normalizeExistingCatalog();
//     }

//     private ServiceCategory category(String name, String slug, String description, int sortOrder) {
//         return serviceCategoryRepository.findBySlug(slug)
//                 .orElseGet(() -> serviceCategoryRepository.save(new ServiceCategory(name, slug, description, sortOrder)));
//     }

//     private void accountStock(String name, String slug, ServiceCategory category, String description, long price,
//             int sortOrder) {
//         boolean exists = serviceItemRepository.findBySlug(slug).isPresent();
//         ServiceItem service = serviceItemRepository.findBySlug(slug)
//                 .orElseGet(() -> serviceItemRepository.save(new ServiceItem(
//                         name,
//                         slug,
//                         description,
//                         description,
//                         BigDecimal.valueOf(price),
//                         ServiceType.ACCOUNT_STOCK,
//                         ServiceStatus.ACTIVE)));
//         service.updateCategory(category);
//         service.updateType(ServiceType.ACCOUNT_STOCK);
//         service.updatePricingMetadata(exists ? service.getStockStatus() : ServiceStockStatus.OUT_OF_STOCK,
//                 ServiceCtaType.BUY_NOW, "Giao tự động", service.isFeatured(), service.isPublicVisible());
//         service.updateProcessingTime("Giao tự động sau thanh toán");
//         service.updateWarrantyPolicy("Bảo hành theo thời hạn hiển thị trên tài khoản được giao");
//         service.updateSortOrder(sortOrder);
//     }

//     private void manual(String name, String slug, ServiceCategory category, String description, long price,
//             int sortOrder) {
//         ServiceItem service = serviceItemRepository.findBySlug(slug)
//                 .orElseGet(() -> serviceItemRepository.save(new ServiceItem(
//                         name,
//                         slug,
//                         description,
//                         description,
//                         BigDecimal.valueOf(price),
//                         ServiceType.MANUAL,
//                         ServiceStatus.ACTIVE)));
//         service.updateCategory(category);
//         service.updateType(ServiceType.MANUAL);
//         service.updatePricingMetadata(ServiceStockStatus.AVAILABLE, ServiceCtaType.BUY_NOW,
//                 "Thủ công", service.isFeatured(), service.isPublicVisible());
//         service.updateInputSchema(facebookBriefSchema());
//         service.updateProcessingTime("Admin xử lý sau khi nhận brief");
//         service.updateWarrantyPolicy("Hỗ trợ theo phạm vi brief đã thống nhất");
//         service.updateSortOrder(sortOrder);
//     }

//     private void normalizeExistingCatalog() {
//         List<String> accountKeywords = List.of("chatgpt", "claude", "gemini", "midjourney", "canva", "capcut", "netflix");
//         List<String> manualKeywords = List.of("facebook", "ads", "quảng cáo", "quang cao", "setup", "tư vấn", "tu van");
//         serviceItemRepository.findAll().forEach(service -> {
//             String text = (service.getName() + " " + service.getSlug()).toLowerCase(Locale.ROOT);
//             if (accountKeywords.stream().anyMatch(text::contains)) {
//                 service.updateType(ServiceType.ACCOUNT_STOCK);
//                 if (service.getStockStatus() == ServiceStockStatus.CONSULTING_ONLY) {
//                     service.updatePricingMetadata(ServiceStockStatus.OUT_OF_STOCK, ServiceCtaType.BUY_NOW,
//                             service.getPricingBadge(), service.isFeatured(), service.isPublicVisible());
//                 }
//             } else if (manualKeywords.stream().anyMatch(text::contains)) {
//                 service.updateType(ServiceType.MANUAL);
//                 service.updatePricingMetadata(ServiceStockStatus.AVAILABLE, ServiceCtaType.BUY_NOW,
//                         service.getPricingBadge(), service.isFeatured(), service.isPublicVisible());
//             }
//         });
//     }

//     private String facebookBriefSchema() {
//         return """
//                 {"type":"object","required":["facebookUrl","goal"],"properties":{"facebookUrl":{"type":"string","label":"Link fanpage / website"},"goal":{"type":"string","label":"Mục tiêu"},"budget":{"type":"string","label":"Ngân sách dự kiến"},"audience":{"type":"string","label":"Tệp khách hàng"},"note":{"type":"string","label":"Nội dung / ghi chú"}}}
//                 """;
//     }
// }
