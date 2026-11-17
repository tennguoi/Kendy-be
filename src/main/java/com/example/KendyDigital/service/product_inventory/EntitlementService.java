package com.example.KendyDigital.service.product_inventory;

import com.example.KendyDigital.dto.entitlement.request.AdminEntitlementUpdateRequest;
import com.example.KendyDigital.dto.entitlement.response.UserEntitlementResponse;
import com.example.KendyDigital.model.inventory.AccountCredential;
import com.example.KendyDigital.model.order.OrderRecord;
import java.util.List;
import java.util.UUID;

public interface EntitlementService {
    void createForOrder(OrderRecord order, AccountCredential credential);
    void activateForOrder(OrderRecord order);
    void revokeForOrder(OrderRecord order, String reason);
    List<UserEntitlementResponse> listForUser(Long userId, Integer limit);
    List<UserEntitlementResponse> listForAdmin(Integer limit);
    UserEntitlementResponse requestRenewal(Long userId, UUID entitlementId);
    UserEntitlementResponse updateByAdmin(Long adminUserId, UUID entitlementId,
            AdminEntitlementUpdateRequest request);
    int backfillExistingOrders();
    int processLifecycle();
}
