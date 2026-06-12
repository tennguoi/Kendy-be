package com.example.KendyDigital.service;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.KendyDigital.dto.finance.request.AdminDepositCancelRequest;
import com.example.KendyDigital.dto.finance.request.AdminDepositExtendRequest;
import com.example.KendyDigital.dto.deposit.response.DepositResponse;
import com.example.KendyDigital.dto.finance.request.ManualCreditDepositRequest;
import com.example.KendyDigital.model.DepositRequest;
import com.example.KendyDigital.model.DepositStatus;
import com.example.KendyDigital.model.UserAccount;
import com.example.KendyDigital.model.UserStatus;
import com.example.KendyDigital.model.WalletTransaction;
import com.example.KendyDigital.model.WalletTransactionType;
import com.example.KendyDigital.repository.DepositRequestRepository;
import com.example.KendyDigital.repository.UserAccountRepository;

@Service
public class AdminDepositManagerService {
    private final DepositRequestRepository depositRequestRepository;
    private final UserAccountRepository userAccountRepository;
    private final WalletLedgerService walletLedgerService;
    private final AuditService auditService;

    public AdminDepositManagerService(DepositRequestRepository depositRequestRepository,
            UserAccountRepository userAccountRepository,
            WalletLedgerService walletLedgerService,
            AuditService auditService) {
        this.depositRequestRepository = depositRequestRepository;
        this.userAccountRepository = userAccountRepository;
        this.walletLedgerService = walletLedgerService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<DepositResponse> listDeposits(DepositStatus status, Long userId) {
        return listDeposits(status, userId, null, null, 0, 100);
    }

    @Transactional(readOnly = true)
    public List<DepositResponse> listDeposits(DepositStatus status, Long userId, Instant fromDate, Instant toDate, int page, int size) {
        List<DepositRequest> deposits = userId != null
                ? depositRequestRepository.findAllByUser_IdOrderByCreatedAtDesc(userId, paged(page, size))
                : status == null
                        ? depositRequestRepository.findAllByOrderByCreatedAtDesc(paged(page, size))
                        : depositRequestRepository.findAllByStatusOrderByCreatedAtDesc(status, paged(page, size));
        return deposits.stream().map(DepositResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<DepositResponse> listDeposits(DepositStatus status, Long userId, Integer limit) {
        return listDeposits(status, userId, null, null, 0, limit == null ? 100 : limit);
    }

    @Transactional(readOnly = true)
    public List<DepositResponse> searchDeposits(String query, DepositStatus status, Long userId,
            Instant fromDate, Instant toDate, int page, int size) {
        String normalizedQuery = normalizeQuery(query);
        List<DepositRequest> deposits = depositRequestRepository.searchAdmin(
                likePattern(normalizedQuery),
                parseLongOrNull(normalizedQuery),
                status,
                userId,
                paged(page, size));
        return deposits.stream().map(DepositResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<DepositResponse> searchDeposits(String query, DepositStatus status, Long userId, Integer limit) {
        return searchDeposits(query, status, userId, null, null, 0, limit == null ? 100 : limit);
    }

    @Transactional(readOnly = true)
    public DepositResponse getDepositForAdmin(String depositCode) {
        return depositRequestRepository.findByDepositCode(normalizeDepositCode(depositCode))
                .map(DepositResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Deposit request not found"));
    }

    @Transactional
    public DepositResponse cancelDeposit(Long adminUserId, String depositCode, AdminDepositCancelRequest request) {
        DepositRequest deposit = depositRequestRepository.findByDepositCodeForUpdate(normalizeDepositCode(depositCode))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Deposit request not found"));
        ensureDepositCanBeChanged(deposit, "Completed deposit cannot be cancelled");
        if (deposit.getStatus() != DepositStatus.CANCELLED) {
            deposit.cancel();
            auditService.recordAdmin(
                    adminUserId,
                    "DEPOSIT_CANCELLED_BY_ADMIN",
                    "DEPOSIT_REQUEST",
                    deposit.getId(),
                    "reason=" + request.reason());
        }
        return DepositResponse.from(deposit);
    }

    @Transactional
    public DepositResponse extendDeposit(Long adminUserId, String depositCode, AdminDepositExtendRequest request) {
        DepositRequest deposit = depositRequestRepository.findByDepositCodeForUpdate(normalizeDepositCode(depositCode))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Deposit request not found"));
        ensureDepositCanBeChanged(deposit, "Completed deposit cannot be extended");
        if (deposit.getStatus() == DepositStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cancelled deposit cannot be extended");
        }

        Instant baseTime = deposit.getExpiredAt().isAfter(Instant.now()) ? deposit.getExpiredAt() : Instant.now();
        deposit.extendTo(baseTime.plusSeconds(request.minutes() * 60L));
        auditService.recordAdmin(
                adminUserId,
                "DEPOSIT_EXTENDED",
                "DEPOSIT_REQUEST",
                deposit.getId(),
                "minutes=" + request.minutes() + ",reason=" + request.reason());
        return DepositResponse.from(deposit);
    }

    @Transactional
    public DepositResponse manualCreditDeposit(Long adminUserId, String depositCode,
            ManualCreditDepositRequest request) {
        DepositRequest deposit = depositRequestRepository.findByDepositCodeForUpdate(normalizeDepositCode(depositCode))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Deposit request not found"));
        ensureDepositCanBeChanged(deposit, "Deposit has already been credited");
        if (deposit.getStatus() == DepositStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cancelled deposit cannot be credited");
        }
        if (deposit.getMatchedBankTransaction() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Use bank transaction manual-credit for deposits with a matched bank transaction");
        }

        UserAccount user = userAccountRepository.findByIdForUpdate(deposit.getUser().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not active");
        }

        WalletTransaction walletTransaction = walletLedgerService.credit(
                user,
                deposit.getAmount(),
                WalletTransactionType.DEPOSIT,
                "DEPOSIT_REQUEST",
                deposit.getId(),
                "Manual deposit credit " + deposit.getDepositCode() + ": " + request.reason(),
                adminUserId);
        deposit.complete(null, walletTransaction);

        auditService.recordAdmin(
                adminUserId,
                "DEPOSIT_MANUAL_CREDITED",
                "DEPOSIT_REQUEST",
                deposit.getId(),
                "userId=" + user.getId() + ",walletTransactionId=" + walletTransaction.getId()
                        + ",reason=" + request.reason());
        return DepositResponse.from(deposit);
    }

    private void ensureDepositCanBeChanged(DepositRequest deposit, String message) {
        if (deposit.getStatus() == DepositStatus.COMPLETED || deposit.getWalletTransaction() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, message);
        }
    }

    private String normalizeDepositCode(String depositCode) {
        if (depositCode == null || depositCode.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Deposit code is required");
        }
        return depositCode.trim().toUpperCase(Locale.ROOT);
    }

    private PageRequest paged(int page, int size) {
        return PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 200)));
    }

    private String normalizeQuery(String query) {
        return query == null || query.isBlank() ? null : query.trim();
    }

    private String likePattern(String query) {
        return query == null ? null : "%" + query.toLowerCase(Locale.ROOT) + "%";
    }

    private Long parseLongOrNull(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
