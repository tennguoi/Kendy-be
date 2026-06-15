package com.example.KendyDigital.service.deposit;

import com.example.KendyDigital.dto.deposit.request.CancelDepositRequest;
import com.example.KendyDigital.dto.deposit.request.CreateDepositRequest;
import com.example.KendyDigital.dto.deposit.response.DepositQrResponse;
import com.example.KendyDigital.dto.deposit.response.DepositResponse;
import com.example.KendyDigital.dto.deposit.response.DepositStatusResponse;
import com.example.KendyDigital.model.deposit.DepositRequest;
import com.example.KendyDigital.model.deposit.DepositStatus;
import java.math.BigDecimal;
import java.util.List;

public interface DepositService {
    DepositResponse createDeposit(Long userId, CreateDepositRequest request);
    DepositRequest createDepositForAmount(Long userId, BigDecimal rawAmount);
    DepositResponse getDepositForUser(Long userId, String depositCode);
    List<DepositResponse> listByUser(Long userId, DepositStatus status);
    List<DepositResponse> listByUser(Long userId, DepositStatus status, int page, int size);
    DepositStatusResponse statusForUser(Long userId, String depositCode);
    DepositQrResponse qrForUser(Long userId, String depositCode);
    DepositResponse cancelForUser(Long userId, String depositCode, CancelDepositRequest request);
}
