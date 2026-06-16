package com.example.KendyDigital;

import com.example.KendyDigital.dto.auth.request.AuthRegisterRequest;
import com.example.KendyDigital.dto.catalog.request.CreateServiceCategoryRequest;
import com.example.KendyDigital.dto.catalog.request.CreateServiceRequest;
import com.example.KendyDigital.dto.checkout.request.CreateServiceCheckoutRequest;
import com.example.KendyDigital.dto.coupon.request.UpsertCouponRequest;
import com.example.KendyDigital.dto.finance.request.ReprocessBankTransactionRequest;
import com.example.KendyDigital.dto.inventory.request.CreateAccountCredentialRequest;
import com.example.KendyDigital.dto.order.request.AdminOrderUpdateRequest;
import com.example.KendyDigital.dto.order.request.CreateOrderRequest;
import com.example.KendyDigital.dto.order.request.RefundOrderRequest;
import com.example.KendyDigital.dto.order.request.ReprocessOrderRequest;
import com.example.KendyDigital.dto.order.response.OrderResponse;
import com.example.KendyDigital.model.bank.BankTransaction;
import com.example.KendyDigital.model.bank.BankTransactionStatus;
import com.example.KendyDigital.model.catalog.ServiceCtaType;
import com.example.KendyDigital.model.catalog.ServiceStatus;
import com.example.KendyDigital.model.catalog.ServiceStockStatus;
import com.example.KendyDigital.model.catalog.ServiceType;
import com.example.KendyDigital.model.checkout.CheckoutStatus;
import com.example.KendyDigital.model.coupon.CouponType;
import com.example.KendyDigital.model.order.OrderRecord;
import com.example.KendyDigital.model.order.OrderStatus;
import com.example.KendyDigital.model.user.UserAccount;
import com.example.KendyDigital.model.user.UserRole;
import com.example.KendyDigital.model.user.UserStatus;
import com.example.KendyDigital.repository.AccountCredentialRepository;
import com.example.KendyDigital.repository.BankTransactionRepository;
import com.example.KendyDigital.repository.OrderRepository;
import com.example.KendyDigital.repository.ServiceItemRepository;
import com.example.KendyDigital.repository.UserAccountRepository;
import com.example.KendyDigital.service.auth.AuthService;
import com.example.KendyDigital.service.bank.AdminBankTxManagerService;
import com.example.KendyDigital.service.catalog.ServiceCatalogService;
import com.example.KendyDigital.service.catalog.ServiceCategoryService;
import com.example.KendyDigital.service.checkout.CheckoutService;
import com.example.KendyDigital.service.coupon.CouponService;
import com.example.KendyDigital.service.inventory.AccountInventoryService;
import com.example.KendyDigital.service.order.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.lang.reflect.Constructor;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class OrderRaceConditionIntegrationTest {

    @Autowired private OrderService orderService;
    @Autowired private CheckoutService checkoutService;
    @Autowired private CouponService couponService;
    @Autowired private AdminBankTxManagerService bankTxManagerService;
    @Autowired private AccountInventoryService inventoryService;
    @Autowired private ServiceCatalogService catalogService;
    @Autowired private ServiceCategoryService categoryService;
    @Autowired private AuthService authService;
    @Autowired private UserAccountRepository userAccountRepository;
    @Autowired private ServiceItemRepository serviceItemRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private AccountCredentialRepository credentialRepository;
    @Autowired private BankTransactionRepository bankTransactionRepository;
    @Autowired private PlatformTransactionManager transactionManager;

    private UserAccount buyer1;
    private UserAccount buyer2;
    private UserAccount buyer3;
    private UserAccount adminUser;
    private Long stockServiceId;
    private static final AtomicInteger counter = new AtomicInteger(0);

    @BeforeEach
    void setUp() {
        counter.incrementAndGet();
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.executeWithoutResult(status -> {
            buyer1 = createUser("buyer1_" + counter, "buyer1_" + counter + "@test.com");
            buyer2 = createUser("buyer2_" + counter, "buyer2_" + counter + "@test.com");
            buyer3 = createUser("buyer3_" + counter, "buyer3_" + counter + "@test.com");
            adminUser = createAdmin("admin_" + counter, "admin_" + counter + "@test.com");

            var catReq = new CreateServiceCategoryRequest("Cat " + counter, "cat-" + counter, null, 0, null);
            var cat = categoryService.create(adminUser.getId(), catReq);

            var svcReq = new CreateServiceRequest(
                    "Svc " + counter, "svc-" + counter,
                    "Short", "Full",
                    BigDecimal.valueOf(100_000), "100.000đ", BigDecimal.valueOf(50_000),
                    ServiceType.ACCOUNT_STOCK, ServiceStatus.ACTIVE, ServiceStockStatus.AVAILABLE,
                    ServiceCtaType.BUY_NOW, null, false, true, null,
                    "Req", "Benefits", "Notes", "24h", "7 days",
                    0, cat.id(), null, null, null);
            stockServiceId = catalogService.create(adminUser.getId(), svcReq).id();

            for (int i = 0; i < 2; i++) {
                inventoryService.create(adminUser.getId(), stockServiceId,
                        new CreateAccountCredentialRequest(
                                "login_" + counter + "_" + i, "pass_" + i,
                                null, null, null, null, null, null));
            }

            BigDecimal creditAmount = BigDecimal.valueOf(1_000_000).setScale(2, RoundingMode.HALF_UP);
            for (var buyer : List.of(buyer1, buyer2, buyer3)) {
                buyer.setBalance(creditAmount);
                userAccountRepository.save(buyer);
            }
        });
    }

    private UserAccount createUser(String username, String email) {
        var resp = authService.register(
                new AuthRegisterRequest(username, email, null, "password123"));
        return userAccountRepository.findById(resp.id()).orElseThrow();
    }

    private UserAccount createAdmin(String username, String email) {
        var user = new UserAccount(username, email, null, "password123");
        user.setRole(UserRole.ADMIN);
        return userAccountRepository.save(user);
    }

    @Test
    @DisplayName("Concurrent purchase with limited credentials: at most available count should succeed")
    void concurrentPurchase_limitedCredentials_onlyAvailableSucceed() throws Exception {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        long available = tx.execute(s -> inventoryService.availableCount(stockServiceId));
        assertEquals(2L, available);

        int threadCount = 4;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CyclicBarrier barrier = new CyclicBarrier(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        List<UserAccount> buyers = List.of(buyer1, buyer2, buyer3);

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            UserAccount buyer = buyers.get(i % buyers.size());
            futures.add(executor.submit(() -> {
                try {
                    barrier.await(10, TimeUnit.SECONDS);
                    orderService.create(buyer.getId(),
                            new CreateOrderRequest(stockServiceId, null, null, null));
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                }
            }));
        }

        for (Future<?> f : futures) {
            try { f.get(30, TimeUnit.SECONDS); } catch (Exception ignored) {}
        }
        executor.shutdown();

        long remainingAvailable = tx.execute(s -> inventoryService.availableCount(stockServiceId));
        assertTrue(successCount.get() <= 2, "At most 2 purchases should succeed (matching 2 available credentials)");
        assertTrue(successCount.get() >= 1, "At least 1 purchase should succeed");
        assertEquals(available - successCount.get(), remainingAvailable,
                "Taken credentials should match successful purchases");
    }

    @Test
    @DisplayName("Concurrent refund on same order: only first refund should succeed")
    void concurrentRefund_sameOrder_onlyFirstSucceeds() throws Exception {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        OrderResponse order = tx.execute(s -> orderService.create(buyer1.getId(),
                new CreateOrderRequest(stockServiceId, null, null, null)));
        assertNotNull(order);

        int threadCount = 3;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CyclicBarrier barrier = new CyclicBarrier(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                try {
                    barrier.await(10, TimeUnit.SECONDS);
                    orderService.refund(order.orderCode(), adminUser.getId(),
                            new RefundOrderRequest("Race refund"));
                    successCount.incrementAndGet();
                } catch (Exception ignored) {
                }
            }));
        }

        for (Future<?> f : futures) {
            try { f.get(30, TimeUnit.SECONDS); } catch (Exception ignored) {}
        }
        executor.shutdown();

        assertEquals(1, successCount.get(), "Only one refund should succeed");
        OrderRecord finalOrder = tx.execute(s ->
                orderRepository.findByOrderCodeForUpdate(order.orderCode()).orElseThrow());
        assertEquals(OrderStatus.REFUNDED, finalOrder.getStatus());
    }

    @Test
    @DisplayName("Concurrent reprocess on refunded order: at most one should succeed")
    void concurrentReprocess_onlyFirstSucceeds() throws Exception {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        OrderResponse order = tx.execute(s -> orderService.create(buyer1.getId(),
                new CreateOrderRequest(stockServiceId, null, null, null)));
        assertNotNull(order);

        tx.executeWithoutResult(s -> {
            orderService.refund(order.orderCode(), adminUser.getId(),
                    new RefundOrderRequest("Initial refund"));
        });

        int threadCount = 3;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CyclicBarrier barrier = new CyclicBarrier(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                try {
                    barrier.await(10, TimeUnit.SECONDS);
                    orderService.reprocess(order.orderCode(), adminUser.getId(),
                            new ReprocessOrderRequest("Race reprocess"));
                    successCount.incrementAndGet();
                } catch (Exception ignored) {
                }
            }));
        }

        for (Future<?> f : futures) {
            try { f.get(30, TimeUnit.SECONDS); } catch (Exception ignored) {}
        }
        executor.shutdown();

        assertTrue(successCount.get() <= 1, "At most one reprocess should succeed");
    }

    @Test
    @DisplayName("Concurrent credential replacement for warranty: first replacement succeeds")
    void concurrentCredentialReplace_forWarranty_firstSucceeds() throws Exception {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        // Order is auto-completed with credential delivery during create() for ACCOUNT_STOCK
        OrderResponse order = tx.execute(s -> orderService.create(buyer1.getId(),
                new CreateOrderRequest(stockServiceId, null, null, null)));
        assertNotNull(order);
        assertEquals(OrderStatus.COMPLETED.name(), order.status().name());

        int threadCount = 3;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CyclicBarrier barrier = new CyclicBarrier(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                try {
                    barrier.await(10, TimeUnit.SECONDS);
                    tx.executeWithoutResult(s -> {
                        OrderRecord lockedOrder = orderRepository
                                .findByOrderCodeForUpdate(order.orderCode()).orElseThrow();
                        inventoryService.replaceForOrder(adminUser.getId(), lockedOrder, null);
                    });
                    successCount.incrementAndGet();
                } catch (Exception ignored) {
                }
            }));
        }

        for (Future<?> f : futures) {
            try { f.get(30, TimeUnit.SECONDS); } catch (Exception ignored) {}
        }
        executor.shutdown();

        assertEquals(1, successCount.get(), "Only one credential replacement should succeed");
    }

    @Test
    @DisplayName("Wallet purchase applies coupon discount and enforces per-user limit")
    void walletPurchase_withCoupon_appliesDiscountAndUserLimit() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        String couponCode = "SAVE10-" + counter.get();

        tx.executeWithoutResult(s -> couponService.create(adminUser.getId(), new UpsertCouponRequest(
                couponCode,
                "Save 10 percent",
                CouponType.PERCENT,
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(20_000),
                BigDecimal.valueOf(50_000),
                10,
                1,
                null,
                null,
                null,
                stockServiceId,
                "Test coupon")));

        OrderResponse order = tx.execute(s -> orderService.create(buyer1.getId(),
                new CreateOrderRequest(stockServiceId, null, null, couponCode)));

        assertNotNull(order);
        assertEquals(new BigDecimal("100000.00"), order.originalAmount());
        assertEquals(new BigDecimal("10000.00"), order.discountAmount());
        assertEquals(new BigDecimal("90000.00"), order.amount());
        assertEquals(couponCode, order.couponCode());

        assertThrows(Exception.class, () -> tx.execute(s -> orderService.create(buyer1.getId(),
                new CreateOrderRequest(stockServiceId, null, null, couponCode))));
    }

    @Test
    @DisplayName("Bank checkout keeps reserved credential and creates order after reprocess")
    void bankCheckout_reservedCredential_reprocessCreatesOrder() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        var checkout = tx.execute(s -> checkoutService.createServiceCheckout(buyer1.getId(),
                new CreateServiceCheckoutRequest(stockServiceId, null, "checkout-bank-" + counter.get(), null)));
        assertNotNull(checkout);
        assertEquals(CheckoutStatus.PENDING_PAYMENT, checkout.status());
        assertEquals(1L, tx.execute(s -> inventoryService.availableCount(stockServiceId)).longValue());

        Long bankTransactionId = insertBankTransaction(
                checkout.deposit().depositCode(),
                checkout.amount(),
                "bank-checkout-" + counter.get());

        var reprocessed = tx.execute(s -> bankTxManagerService.reprocessBankTransaction(
                adminUser.getId(),
                bankTransactionId,
                new ReprocessBankTransactionRequest(checkout.deposit().depositCode(), "Test bank reprocess")));
        assertNotNull(reprocessed);
        assertEquals(BankTransactionStatus.CREDITED, reprocessed.status());
        assertNotNull(reprocessed.walletTransactionId());

        var refreshed = tx.execute(s -> checkoutService.getStatus(buyer1.getId(), checkout.checkoutCode()));
        assertEquals(CheckoutStatus.ORDER_CREATED, refreshed.status());
        assertNotNull(refreshed.order());
        assertEquals(OrderStatus.COMPLETED, refreshed.order().status());
        assertEquals(1L, tx.execute(s -> inventoryService.availableCount(stockServiceId)).longValue());

        OrderRecord order = tx.execute(s -> orderRepository
                .findByOrderCodeForUpdate(refreshed.order().orderCode()).orElseThrow());
        assertNotNull(order.getDeliveredCredential());
        assertEquals(buyer1.getId(), order.getDeliveredCredential().getDeliveredToUser().getId());
        assertNull(order.getDeliveredCredential().getReservedCheckout());
    }

    @Test
    @DisplayName("Concurrent bank reprocess on same transaction credits only once")
    void concurrentBankReprocess_sameTransaction_onlyFirstCredits() throws Exception {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        var checkout = tx.execute(s -> checkoutService.createServiceCheckout(buyer2.getId(),
                new CreateServiceCheckoutRequest(stockServiceId, null, "checkout-race-" + counter.get(), null)));
        Long bankTransactionId = insertBankTransaction(
                checkout.deposit().depositCode(),
                checkout.amount(),
                "bank-race-" + counter.get());

        int threadCount = 3;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CyclicBarrier barrier = new CyclicBarrier(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                try {
                    barrier.await(10, TimeUnit.SECONDS);
                    bankTxManagerService.reprocessBankTransaction(
                            adminUser.getId(),
                            bankTransactionId,
                            new ReprocessBankTransactionRequest(checkout.deposit().depositCode(),
                                    "Race bank reprocess"));
                    successCount.incrementAndGet();
                } catch (Exception ignored) {
                }
            }));
        }

        for (Future<?> f : futures) {
            try { f.get(30, TimeUnit.SECONDS); } catch (Exception ignored) {}
        }
        executor.shutdown();

        assertEquals(1, successCount.get(), "Only one bank reprocess should credit the wallet");
        var bankTransaction = tx.execute(s -> bankTransactionRepository.findById(bankTransactionId).orElseThrow());
        assertEquals(BankTransactionStatus.CREDITED, bankTransaction.getStatus());
        assertNotNull(bankTransaction.getWalletTransaction());
    }

    private Long insertBankTransaction(String depositCode, BigDecimal amount, String referenceCode) {
        try {
            Constructor<BankTransaction> constructor = BankTransaction.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            BankTransaction transaction = constructor.newInstance();
            transaction.setSepayId(Math.abs(referenceCode.hashCode()) + 10_000L);
            transaction.setGateway("MBBank");
            transaction.setBankName("MBBank");
            transaction.setAccountNumber("0123456789");
            transaction.setTransactionDate(Instant.now());
            transaction.setTransferType("IN");
            transaction.setTransferAmount(amount);
            transaction.setAccumulated(amount);
            transaction.setCode(depositCode);
            transaction.setContent("Thanh toan " + depositCode);
            transaction.setReferenceCode(referenceCode);
            transaction.setStatus(BankTransactionStatus.NEW);
            transaction.setRawPayload("{}");
            transaction.setReceivedAt(Instant.now());
            return bankTransactionRepository.saveAndFlush(transaction).getId();
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot create test bank transaction", exception);
        }
    }
}
