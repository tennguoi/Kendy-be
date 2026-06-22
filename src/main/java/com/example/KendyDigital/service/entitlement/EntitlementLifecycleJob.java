package com.example.KendyDigital.service.entitlement;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EntitlementLifecycleJob implements ApplicationRunner {
    private final EntitlementService entitlementService;

    public EntitlementLifecycleJob(EntitlementService entitlementService) {
        this.entitlementService = entitlementService;
    }

    @Override
    public void run(ApplicationArguments args) {
        entitlementService.backfillExistingOrders();
        entitlementService.processLifecycle();
    }

    @Scheduled(fixedDelayString = "${app.entitlement.lifecycle-interval-ms:60000}")
    public void process() {
        entitlementService.processLifecycle();
    }
}
