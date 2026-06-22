package com.example.KendyDigital.controller;

import com.example.KendyDigital.dto.entitlement.request.AdminEntitlementUpdateRequest;
import com.example.KendyDigital.dto.entitlement.response.UserEntitlementResponse;
import com.example.KendyDigital.security.CurrentUser;
import com.example.KendyDigital.service.entitlement.EntitlementService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EntitlementController {
    private final EntitlementService entitlementService;

    public EntitlementController(EntitlementService entitlementService) {
        this.entitlementService = entitlementService;
    }

    @GetMapping("/api/me/entitlements")
    public List<UserEntitlementResponse> list(Authentication authentication,
            @RequestParam(required = false) Integer limit) {
        return entitlementService.listForUser(CurrentUser.require(authentication).userId(), limit);
    }

    @PostMapping("/api/me/entitlements/{id}/renew")
    public UserEntitlementResponse requestRenewal(Authentication authentication, @PathVariable Long id) {
        return entitlementService.requestRenewal(CurrentUser.require(authentication).userId(), id);
    }

    @PatchMapping("/api/admin/entitlements/{id}")
    public UserEntitlementResponse update(Authentication authentication, @PathVariable Long id,
            @Valid @RequestBody AdminEntitlementUpdateRequest request) {
        return entitlementService.updateByAdmin(CurrentUser.require(authentication).userId(), id, request);
    }

    @GetMapping("/api/admin/entitlements")
    public List<UserEntitlementResponse> listForAdmin(@RequestParam(required = false) Integer limit) {
        return entitlementService.listForAdmin(limit);
    }
}
