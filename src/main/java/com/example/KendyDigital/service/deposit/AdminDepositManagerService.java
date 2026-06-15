package com.example.KendyDigital.service.deposit;

import com.example.KendyDigital.dto.deposit.response.DepositResponse;
import com.example.KendyDigital.dto.finance.request.AdminDepositCancelRequest;
import com.example.KendyDigital.dto.finance.request.AdminDepositExtendRequest;
import com.example.KendyDigital.dto.finance.request.ManualCreditDepositRequest;
import com.example.KendyDigital.model.deposit.DepositStatus;
import java.time.Instant;
import java.util.List;

public interface AdminDepositManagerService {
    List<DepositResponse> listDeposits(DepositStatus status, Long userId);
    List<DepositResponse> listDeposits(DepositStatus status, Long userId, Instant fromDate, Instant toDate, int page, int size);
    List<DepositResponse> listDeposits(DepositStatus status, Long userId, Integer limit);
    List<DepositResponse> searchDeposits(String query, DepositStatus status, Long userId, Instant fromDate, Instant toDate, int page, int size);
    List<DepositResponse> searchDeposits(String query, DepositStatus status, Long userId, Integer limit);
    DepositResponse getDepositForAdmin(String depositCode);
    DepositResponse cancelDeposit(Long adminUserId, String depositCode, AdminDepositCancelRequest request);
    DepositResponse extendDeposit(Long adminUserId, String depositCode, AdminDepositExtendRequest request);
    DepositResponse manualCreditDeposit(Long adminUserId, String depositCode, ManualCreditDepositRequest request);
}
