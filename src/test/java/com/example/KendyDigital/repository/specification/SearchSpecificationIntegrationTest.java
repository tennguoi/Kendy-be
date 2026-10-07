package com.example.KendyDigital.repository.specification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.KendyDigital.model.catalog.ServiceCategory;
import com.example.KendyDigital.model.catalog.ServiceItem;
import com.example.KendyDigital.model.catalog.ServiceStatus;
import com.example.KendyDigital.model.catalog.ServiceType;
import com.example.KendyDigital.model.deposit.DepositRequest;
import com.example.KendyDigital.model.deposit.DepositStatus;
import com.example.KendyDigital.model.order.OrderRecord;
import com.example.KendyDigital.model.order.OrderStatus;
import com.example.KendyDigital.model.user.UserAccount;
import com.example.KendyDigital.model.user.UserRole;
import com.example.KendyDigital.model.user.UserStatus;
import com.example.KendyDigital.model.security.SecurityEvent;
import com.example.KendyDigital.model.security.SecurityEventType;
import com.example.KendyDigital.model.security.SecuritySeverity;
import com.example.KendyDigital.repository.DepositRequestRepository;
import com.example.KendyDigital.repository.OrderRepository;
import com.example.KendyDigital.repository.SecurityEventRepository;
import com.example.KendyDigital.repository.ServiceCategoryRepository;
import com.example.KendyDigital.repository.ServiceItemRepository;
import com.example.KendyDigital.repository.UserAccountRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class SearchSpecificationIntegrationTest {

    @Autowired
    private ServiceItemRepository serviceItemRepository;

    @Autowired
    private ServiceCategoryRepository serviceCategoryRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private DepositRequestRepository depositRequestRepository;

    @Autowired
    private SecurityEventRepository securityEventRepository;

    private ServiceCategory testCategory;
    private ServiceItem testService1;
    private ServiceItem testService2;
    private UserAccount testUser1;
    private UserAccount testUser2;
    private OrderRecord testOrder1;
    private DepositRequest testDeposit1;

    @BeforeEach
    void setUp() {
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        testCategory = new ServiceCategory(
                "Streaming " + uniqueSuffix,
                "streaming-" + uniqueSuffix,
                "Streaming services description",
                1);
        testCategory = serviceCategoryRepository.save(testCategory);

        testService1 = new ServiceItem(
                "Netflix Premium 4K " + uniqueSuffix,
                "netflix-prem-" + uniqueSuffix,
                "Best 4K UHD streaming account",
                "Full description of netflix premium",
                BigDecimal.valueOf(65000),
                ServiceType.ACCOUNT_STOCK,
                ServiceStatus.ACTIVE);
        testService1.updateCategory(testCategory);
        testService1.setFeatured(true);
        testService1.setPublicVisible(true);
        testService1 = serviceItemRepository.save(testService1);

        testService2 = new ServiceItem(
                "Spotify Family Slot " + uniqueSuffix,
                "spotify-fam-" + uniqueSuffix,
                "Music on demand without ads",
                "Full description of spotify family",
                BigDecimal.valueOf(35000),
                ServiceType.ACCOUNT_STOCK,
                ServiceStatus.ACTIVE);
        testService2.updateCategory(testCategory);
        testService2.setFeatured(false);
        testService2.setPublicVisible(true);
        testService2 = serviceItemRepository.save(testService2);

        testUser1 = new UserAccount(
                "Nguyen Van Tester " + uniqueSuffix,
                "tester." + uniqueSuffix + "@kendydigital.com",
                "0987654321",
                "hashedpassword");
        testUser1.setRole(UserRole.USER);
        testUser1.setStatus(UserStatus.ACTIVE);
        testUser1 = userAccountRepository.save(testUser1);

        testUser2 = new UserAccount(
                "Tran Admin Locked " + uniqueSuffix,
                "admin.locked." + uniqueSuffix + "@kendydigital.com",
                "0912345678",
                "hashedpassword");
        testUser2.setRole(UserRole.ADMIN);
        testUser2.setStatus(UserStatus.LOCKED);
        testUser2 = userAccountRepository.save(testUser2);

        testOrder1 = new OrderRecord(
                "ORD-" + uniqueSuffix,
                testUser1,
                testService1,
                BigDecimal.valueOf(65000),
                "customer-note-" + uniqueSuffix,
                UUID.randomUUID().toString());
        testOrder1 = orderRepository.save(testOrder1);

        testDeposit1 = new DepositRequest(
                "DEP-" + uniqueSuffix,
                testUser1,
                BigDecimal.valueOf(100000),
                "MBBANK",
                "0987654321",
                "KENDY DIGITAL",
                "DEP-" + uniqueSuffix,
                Instant.now().plusSeconds(1800));
        testDeposit1 = depositRequestRepository.save(testDeposit1);
    }

    @Test
    @DisplayName("ServiceItemSpecification matches name case-insensitively")
    void testServiceSpecificationSearchByName() {
        var spec = ServiceItemSpecifications.searchPublic("netflix", null, null, null);
        List<ServiceItem> results = serviceItemRepository.findAll(spec, PageRequest.of(0, 10)).getContent();

        assertFalse(results.isEmpty());
        assertTrue(results.stream().anyMatch(s -> s.getId().equals(testService1.getId())));
        assertFalse(results.stream().anyMatch(s -> s.getId().equals(testService2.getId())));
    }

    @Test
    @DisplayName("ServiceItemSpecification matches category slug and featured flag")
    void testServiceSpecificationCategoryAndFeatured() {
        var specFeatured = ServiceItemSpecifications.searchPublic(null, null, testCategory.getSlug(), true);
        List<ServiceItem> results = serviceItemRepository.findAll(specFeatured, PageRequest.of(0, 10)).getContent();

        assertTrue(results.stream().anyMatch(s -> s.getId().equals(testService1.getId())));
        assertFalse(results.stream().anyMatch(s -> s.getId().equals(testService2.getId())));
    }

    @Test
    @DisplayName("UserAccountSpecification matches partial email and status correctly")
    void testUserSpecificationSearch() {
        var spec = UserAccountSpecifications.searchAdmin("kendydigital.com", UserStatus.ACTIVE);
        List<UserAccount> results = userAccountRepository.findAll(spec, PageRequest.of(0, 10)).getContent();

        assertTrue(results.stream().anyMatch(u -> u.getId().equals(testUser1.getId())));
        assertFalse(results.stream().anyMatch(u -> u.getId().equals(testUser2.getId())));
    }

    @Test
    @DisplayName("UserAccountSpecification matches by phone number")
    void testUserSpecificationSearchByPhone() {
        var spec = UserAccountSpecifications.searchAdmin("0912345678", null);
        List<UserAccount> results = userAccountRepository.findAll(spec, PageRequest.of(0, 10)).getContent();

        assertTrue(results.stream().anyMatch(u -> u.getId().equals(testUser2.getId())));
    }

    @Test
    @DisplayName("OrderSpecifications matches by orderCode and user email")
    void testOrderSpecificationSearch() {
        var specByCode = OrderSpecifications.searchAdmin(testOrder1.getOrderCode(), null, null, null, null);
        List<OrderRecord> results = orderRepository.findAll(specByCode, PageRequest.of(0, 10)).getContent();
        assertTrue(results.stream().anyMatch(o -> o.getId().equals(testOrder1.getId())));

        var specByUser = OrderSpecifications.searchUser(testUser1.getId(), "netflix", OrderStatus.PROCESSING);
        List<OrderRecord> userOrders = orderRepository.findAll(specByUser, PageRequest.of(0, 10)).getContent();
        assertTrue(userOrders.stream().anyMatch(o -> o.getId().equals(testOrder1.getId())));
    }

    @Test
    @DisplayName("DepositRequestSpecifications matches by depositCode and transferContent")
    void testDepositSpecificationSearch() {
        var spec = DepositRequestSpecifications.searchAdmin(testDeposit1.getDepositCode(), DepositStatus.PENDING, null, null, null);
        List<DepositRequest> results = depositRequestRepository.findAll(spec, PageRequest.of(0, 10)).getContent();

        assertFalse(results.isEmpty());
        assertTrue(results.stream().anyMatch(d -> d.getId().equals(testDeposit1.getId())));
    }

    @Test
    @DisplayName("SecurityEventSpecifications matches by IP, userId, and severity")
    void testSecurityEventSpecificationSearch() {
        SecurityEvent event = new SecurityEvent(
                SecurityEventType.LOGIN_SUCCESS,
                SecuritySeverity.LOW,
                "192.168.1.100",
                testUser1.getId(),
                null,
                "POST",
                "/api/auth/login",
                "Mozilla/5.0",
                "fp-12345",
                "{\"device\":\"Chrome\"}");
        final SecurityEvent savedEvent = securityEventRepository.save(event);

        var spec = SecurityEventSpecifications.searchEvents(
                SecurityEventType.LOGIN_SUCCESS,
                SecuritySeverity.LOW,
                "192.168.1.100",
                testUser1.getId(),
                null,
                null);
        List<SecurityEvent> results = securityEventRepository.findAll(spec, PageRequest.of(0, 10)).getContent();

        assertFalse(results.isEmpty());
        assertTrue(results.stream().anyMatch(e -> e.getId().equals(savedEvent.getId())));
    }
}
