package com.example.KendyDigital.service.customer_service;

import com.example.KendyDigital.dto.warranty.request.AdminWarrantyReviewRequest;
import com.example.KendyDigital.dto.warranty.request.CreateWarrantyRequest;
import com.example.KendyDigital.dto.warranty.response.WarrantyRequestResponse;
import com.example.KendyDigital.model.warranty.WarrantyRequestStatus;
import java.util.List;
import java.util.UUID;

public interface WarrantyService {
    WarrantyRequestResponse create(Long userId, String orderCode, CreateWarrantyRequest request);
    List<WarrantyRequestResponse> listForUser(Long userId, Integer limit);
    List<WarrantyRequestResponse> listForAdmin(WarrantyRequestStatus status, Integer limit);
    WarrantyRequestResponse review(Long adminUserId, UUID warrantyRequestId, AdminWarrantyReviewRequest request);
}
