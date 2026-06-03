package com.example.KendyDigital.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.KendyDigital.common.CodeGenerator;
import com.example.KendyDigital.config.BankProperties;
import com.example.KendyDigital.dto.CancelDepositRequest;
import com.example.KendyDigital.dto.CreateDepositRequest;
import com.example.KendyDigital.dto.DepositResponse;
import com.example.KendyDigital.model.DepositRequest;
import com.example.KendyDigital.model.DepositStatus;
import com.example.KendyDigital.model.UserAccount;
import com.example.KendyDigital.model.UserStatus;
import com.example.KendyDigital.repository.DepositRequestRepository;
import com.example.KendyDigital.repository.UserAccountRepository;

@Service
public class DepositService {
    private final DepositRequestRepository depositRequestRepository;
    private final UserAccountRepository userAccountRepository;
    private final CodeGenerator codeGenerator;
    private final BankProperties bankProperties;
    private final AuditService auditService;

    public DepositService(DepositRequestRepository depositRequestRepository,
            UserAccountRepository userAccountRepository,
            CodeGenerator codeGenerator,
            BankProperties bankProperties,
            AuditService auditService) {
        this.depositRequestRepository = depositRequestRepository;
        this.userAccountRepository = userAccountRepository;
        this.codeGenerator = codeGenerator;
        this.bankProperties = bankProperties;
        this.auditService = auditService;
    }

    @Transactional
    public DepositResponse createDeposit(Long userId, CreateDepositRequest request) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not active");
        }

        BigDecimal amount = request.amount().setScale(2, RoundingMode.HALF_UP);
        String depositCode = nextDepositCode();
        String transferContent = depositCode;
        Instant expiredAt = Instant.now().plus(Duration.ofMinutes(bankProperties.getDepositExpiryMinutes()));

        DepositRequest deposit = new DepositRequest(
                depositCode,
                user,
                amount,
                bankProperties.getName(),
                bankProperties.getAccountNumber(),
                bankProperties.getAccountOwner(),
                transferContent,
                expiredAt);

        return DepositResponse.from(depositRequestRepository.save(deposit));
    }

    @Transactional(readOnly = true)
    public DepositResponse getDepositForUser(Long userId, String depositCode) {
        DepositRequest deposit = depositRequestRepository.findByDepositCode(depositCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Deposit not found"));
        if (!deposit.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Deposit not found");
        }
        return DepositResponse.from(deposit);
    }

    @Transactional(readOnly = true)
    public List<DepositResponse> listByUser(Long userId, DepositStatus status) {
        List<DepositRequest> deposits = status == null
                ? depositRequestRepository.findAllByUser_IdOrderByCreatedAtDesc(userId, PageRequest.of(0, 50))
                : depositRequestRepository.findAllByUser_IdAndStatusOrderByCreatedAtDesc(userId, status, PageRequest.of(0, 50));
        return deposits
                .stream()
                .map(DepositResponse::from)
                .toList();
    }

    @Transactional
    public DepositResponse cancelForUser(Long userId, String depositCode, CancelDepositRequest request) {
        DepositRequest deposit = depositRequestRepository.findByDepositCodeForUpdate(depositCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Deposit not found"));
        if (!deposit.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Deposit not found");
        }
        if (deposit.getStatus() == DepositStatus.COMPLETED || deposit.getWalletTransaction() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Completed deposit cannot be cancelled");
        }
        if (deposit.getStatus() == DepositStatus.MANUAL_REVIEW) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Deposit is under manual review");
        }
        if (deposit.getStatus() != DepositStatus.CANCELLED) {
            deposit.cancel();
            auditService.recordSystem(
                    "DEPOSIT_CANCELLED_BY_USER",
                    "DEPOSIT_REQUEST",
                    deposit.getId(),
                    "userId=" + userId + ",reason=" + blankToNull(request == null ? null : request.reason()));
        }
        return DepositResponse.from(deposit);
    }

    private String nextDepositCode() {
        String code;
        do {
            code = codeGenerator.generate(bankProperties.getTransferPrefix(), 10);
        } while (depositRequestRepository.existsByDepositCode(code));
        return code;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
