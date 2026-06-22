package com.example.KendyDigital.service.entitlement;

import com.example.KendyDigital.dto.entitlement.request.AdminEntitlementUpdateRequest;
import com.example.KendyDigital.dto.entitlement.response.UserEntitlementResponse;
import com.example.KendyDigital.model.inventory.AccountCredential;
import com.example.KendyDigital.model.order.OrderRecord;
import java.util.List;

public interface EntitlementService {
    void createForOrder(OrderRecord order, AccountCredential credential);
    void activateForOrder(OrderRecord order);
    void revokeForOrder(OrderRecord order, String reason);
    List<UserEntitlementResponse> listForUser(Long userId, Integer limit);
    List<UserEntitlementResponse> listForAdmin(Integer limit);
    UserEntitlementResponse requestRenewal(Long userId, Long entitlementId);
    UserEntitlementResponse updateByAdmin(Long adminUserId, Long entitlementId,
            AdminEntitlementUpdateRequest request);
    int backfillExistingOrders();
    int processLifecycle();
}
