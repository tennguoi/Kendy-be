package com.example.KendyDigital.controller;

import com.example.KendyDigital.dto.warranty.request.AdminWarrantyReviewRequest;
import com.example.KendyDigital.dto.warranty.request.CreateWarrantyRequest;
import com.example.KendyDigital.dto.warranty.response.WarrantyRequestResponse;
import com.example.KendyDigital.model.warranty.WarrantyRequestStatus;
import com.example.KendyDigital.security.CurrentUser;
import com.example.KendyDigital.service.warranty.WarrantyService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WarrantyController {
    private final WarrantyService warrantyService;

    public WarrantyController(WarrantyService warrantyService) {
        this.warrantyService = warrantyService;
    }

    @PostMapping("/api/orders/{orderCode}/warranty")
    public WarrantyRequestResponse create(Authentication authentication, @PathVariable String orderCode,
            @Valid @RequestBody CreateWarrantyRequest request) {
        return warrantyService.create(CurrentUser.require(authentication).userId(), orderCode, request);
    }

    @GetMapping("/api/warranty-requests")
    public List<WarrantyRequestResponse> listForUser(Authentication authentication,
            @RequestParam(required = false) Integer limit) {
        return warrantyService.listForUser(CurrentUser.require(authentication).userId(), limit);
    }

    @GetMapping("/api/admin/warranty-requests")
    public List<WarrantyRequestResponse> listForAdmin(@RequestParam(required = false) WarrantyRequestStatus status,
            @RequestParam(required = false) Integer limit) {
        return warrantyService.listForAdmin(status, limit);
    }

    @PostMapping("/api/admin/warranty-requests/{id}/review")
    public WarrantyRequestResponse review(Authentication authentication, @PathVariable Long id,
            @Valid @RequestBody AdminWarrantyReviewRequest request) {
        return warrantyService.review(CurrentUser.require(authentication).userId(), id, request);
    }
}
