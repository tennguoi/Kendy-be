package com.example.KendyDigital.service.warranty;

import com.example.KendyDigital.dto.warranty.request.AdminWarrantyReviewRequest;
import com.example.KendyDigital.dto.warranty.request.CreateWarrantyRequest;
import com.example.KendyDigital.dto.warranty.response.WarrantyRequestResponse;
import com.example.KendyDigital.model.warranty.WarrantyRequestStatus;
import java.util.List;

public interface WarrantyService {
    WarrantyRequestResponse create(Long userId, String orderCode, CreateWarrantyRequest request);
    List<WarrantyRequestResponse> listForUser(Long userId, Integer limit);
    List<WarrantyRequestResponse> listForAdmin(WarrantyRequestStatus status, Integer limit);
    WarrantyRequestResponse review(Long adminUserId, Long warrantyRequestId, AdminWarrantyReviewRequest request);
}
