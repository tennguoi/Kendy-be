package com.example.KendyDigital.service.inventory;

import com.example.KendyDigital.dto.inventory.request.CreateAccountCredentialRequest;
import com.example.KendyDigital.dto.inventory.request.BulkAccountCredentialImportRequest;
import com.example.KendyDigital.dto.inventory.request.UpdateAccountCredentialRequest;
import com.example.KendyDigital.dto.inventory.response.AccountCredentialAdminResponse;
import com.example.KendyDigital.dto.inventory.response.AccountCredentialRevealResponse;
import com.example.KendyDigital.dto.inventory.response.BulkAccountCredentialImportResponse;
import com.example.KendyDigital.dto.inventory.response.InventoryAlertSummaryResponse;
import com.example.KendyDigital.model.order.OrderRecord;
import com.example.KendyDigital.model.checkout.CheckoutSession;
import com.example.KendyDigital.model.inventory.AccountCredential;
import com.example.KendyDigital.model.inventory.AccountCredentialStatus;
import com.example.KendyDigital.model.user.UserAccount;
import com.example.KendyDigital.model.user.UserRole;
import java.time.Instant;
import java.util.List;

public interface AccountInventoryService {
    List<AccountCredentialAdminResponse> listByService(Long serviceId, AccountCredentialStatus status, Integer limit);
    List<AccountCredentialAdminResponse> searchByService(Long serviceId, AccountCredentialStatus status, String query,
            Instant createdFrom, Instant createdTo, Instant deliveredFrom, Instant deliveredTo,
            Instant expiresBefore, Integer limit);
    List<AccountCredentialAdminResponse> searchAssigned(AccountCredentialStatus status, String query,
            Instant deliveredFrom, Instant deliveredTo, Instant expiresBefore, Integer limit);
    AccountCredentialAdminResponse create(Long adminUserId, Long serviceId, CreateAccountCredentialRequest request);
    BulkAccountCredentialImportResponse bulkImport(Long adminUserId, Long serviceId,
            BulkAccountCredentialImportRequest request);
    AccountCredentialAdminResponse update(Long adminUserId, Long credentialId, UpdateAccountCredentialRequest request);
    AccountCredentialAdminResponse disable(Long adminUserId, Long credentialId);
    AccountCredentialRevealResponse reveal(Long adminUserId, UserRole adminRole, Long credentialId);
    AccountCredential takeAvailableForOrder(Long serviceId);
    AccountCredential reserveForCheckout(Long serviceId, CheckoutSession checkout, UserAccount user,
            Instant reservedUntil);
    AccountCredential takeReservedForOrder(Long serviceId, Long checkoutId, Long userId);
    boolean hasUsableReservation(Long serviceId, Long checkoutId, Long userId, Instant now);
    void releaseReservationForCheckout(Long checkoutId);
    int releaseExpiredReservations(Instant now);
    long availableCount(Long serviceId);
    AccountCredential replaceForOrder(Long adminUserId, OrderRecord order, Long replacementCredentialId);
    InventoryAlertSummaryResponse alertSummary(Integer lowStockThreshold, Integer expiringDays);
}
