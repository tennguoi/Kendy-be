package com.example.KendyDigital.service.security.monitor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.KendyDigital.config.SecurityMonitorProperties;
import com.example.KendyDigital.model.security.AlertSubjectType;
import com.example.KendyDigital.model.security.IpBanSource;
import com.example.KendyDigital.model.security.SecurityEventType;
import com.example.KendyDigital.model.security.SecuritySeverity;
import com.example.KendyDigital.repository.AuthSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;

@ExtendWith(MockitoExtension.class)
class DetectionRuleEngineTest {

    @Mock
    private AlertService alertService;
    @Mock
    private IpBanService ipBanService;
    @Mock
    private AuthSessionRepository authSessionRepository;
    @Mock
    private WalletFreezeService walletFreezeService;

    private SecurityMonitorProperties properties;
    private DetectionRuleEngine engine;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        ObjectProvider<StringRedisTemplate> redisProvider = org.mockito.Mockito.mock(ObjectProvider.class);
        lenient().when(redisProvider.getIfAvailable()).thenReturn(null);
        SecurityCounters counters = new SecurityCounters(redisProvider);
        RiskScoringService riskScoringService = new RiskScoringService(counters, new SecurityMonitorProperties());
        properties = new SecurityMonitorProperties();
        engine = new DetectionRuleEngine(counters, riskScoringService, alertService, ipBanService,
                authSessionRepository, properties, walletFreezeService);
    }

    @Test
    void passwordSprayingFromSingleIpTriggersBanAndAlert() {
        properties.getThresholds().setSprayDistinctEmails(3);

        for (int i = 0; i < 3; i++) {
            engine.evaluate(SecuritySignal
                    .of(SecurityEventType.LOGIN_FAILED, SecuritySeverity.LOW, "1.2.3.4")
                    .metadata("emailHash=user" + i)
                    .build());
        }

        verify(ipBanService, times(1)).ban(eq("1.2.3.4"), anyString(), eq(IpBanSource.AUTO_RULE), eq("AUTH-02"),
                isNull(), any());
        verify(alertService).raise(eq("AUTH-02"), eq(SecuritySeverity.HIGH), anyString(),
                eq(AlertSubjectType.IP), eq("1.2.3.4"), anyString());
    }

    @Test
    void scannerPatternsEventuallyBanIp() {
        properties.getThresholds().setScannerRequests(3);

        for (int i = 0; i < 3; i++) {
            engine.evaluate(SecuritySignal
                    .of(SecurityEventType.WAF_SCANNER, SecuritySeverity.HIGH, "6.6.6.6")
                    .metadata("pattern=SCANNER_PATH")
                    .build());
        }

        verify(ipBanService).ban(eq("6.6.6.6"), anyString(), eq(IpBanSource.AUTO_RULE), eq("WAF-BAN"), isNull(), any());
    }

    @Test
    void twoFactorFailuresBelowThresholdDoNotAlert() {
        properties.getThresholds().setTwoFactorFailPerUser(5);

        engine.evaluate(SecuritySignal
                .of(SecurityEventType.TWO_FACTOR_FAILED, SecuritySeverity.MEDIUM, "1.2.3.4")
                .user(42L)
                .metadata("passwordOk=true")
                .build());

        verify(alertService, never()).raise(eq("AUTH-05"), any(), anyString(), any(), anyString(), any());
    }

    @Test
    void accountTakeoverChainFreezesWalletWhenBlockingEnabled() {
        properties.setBlockEnabled(true);
        properties.setDryRun(false);

        engine.evaluate(SecuritySignal.of(SecurityEventType.PASSWORD_RESET, SecuritySeverity.HIGH, "1.2.3.4")
                .user(7L).metadata("admin=false").build());
        engine.evaluate(SecuritySignal.of(SecurityEventType.TWO_FACTOR_DISABLED, SecuritySeverity.HIGH, "1.2.3.4")
                .user(7L).metadata("admin=true").build());

        verify(alertService).raise(eq("AUTH-13"), eq(SecuritySeverity.CRITICAL), anyString(),
                eq(AlertSubjectType.USER), eq("7"), anyString());
        verify(walletFreezeService).freeze(eq(7L), anyString(), isNull());
    }

    @Test
    void accountTakeoverChainDoesNotFreezeInDryRun() {
        properties.setBlockEnabled(true);
        properties.setDryRun(true);

        engine.evaluate(SecuritySignal.of(SecurityEventType.TWO_FACTOR_DISABLED, SecuritySeverity.HIGH, "1.2.3.4")
                .user(8L).metadata("admin=false").build());
        engine.evaluate(SecuritySignal.of(SecurityEventType.PASSWORD_RESET, SecuritySeverity.HIGH, "1.2.3.4")
                .user(8L).metadata("admin=false").build());

        verify(walletFreezeService, never()).freeze(any(), anyString(), any());
    }

    @Test
    void couponUsedAcrossAccountsRaisesAlert() {
        properties.getThresholds().setCouponMultiAccount(3);

        for (long userId = 1; userId <= 3; userId++) {
            engine.evaluate(SecuritySignal.of(SecurityEventType.COUPON_ABUSE, SecuritySeverity.LOW, "9.9.9.9")
                    .user(userId).metadata("couponCode=ABC").build());
        }

        verify(alertService).raise(eq("BIZ-08"), eq(SecuritySeverity.MEDIUM), anyString(),
                eq(AlertSubjectType.IP), eq("9.9.9.9"), anyString());
    }

    @Test
    void repeatedWarrantyRequestsRaiseAlert() {
        properties.getThresholds().setWarrantyRepeat(3);

        for (int i = 0; i < 3; i++) {
            engine.evaluate(SecuritySignal.of(SecurityEventType.WARRANTY_ABUSE, SecuritySeverity.LOW, null)
                    .user(5L).metadata("orderId=1").build());
        }

        verify(alertService).raise(eq("BIZ-09"), eq(SecuritySeverity.MEDIUM), anyString(),
                eq(AlertSubjectType.USER), eq("5"), anyString());
    }
}

