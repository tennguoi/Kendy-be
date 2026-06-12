package com.example.KendyDigital.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.KendyDigital.config.BankProperties;
import com.example.KendyDigital.dto.finance.response.AdminBankTransactionResponse;
import com.example.KendyDigital.dto.finance.request.BulkManualCreditBankTransactionsRequest;
import com.example.KendyDigital.dto.finance.request.IgnoreBankTransactionRequest;
import com.example.KendyDigital.dto.finance.request.ManualCreditBankTransactionRequest;
import com.example.KendyDigital.dto.finance.request.MatchBankTransactionRequest;
import com.example.KendyDigital.dto.finance.request.ReprocessBankTransactionRequest;
import com.example.KendyDigital.model.BankTransaction;
import com.example.KendyDigital.model.BankTransactionStatus;
import com.example.KendyDigital.model.DepositRequest;
import com.example.KendyDigital.model.DepositStatus;
import com.example.KendyDigital.model.UserAccount;
import com.example.KendyDigital.model.UserStatus;
import com.example.KendyDigital.model.WalletTransaction;
import com.example.KendyDigital.model.WalletTransactionType;
import com.example.KendyDigital.repository.BankTransactionRepository;
import com.example.KendyDigital.repository.DepositRequestRepository;
import com.example.KendyDigital.repository.UserAccountRepository;

@Service
public class AdminBankTxManagerService {
    private static final String TRANSFER_IN = "IN";

    private final BankTransactionRepository bankTransactionRepository;
    private final DepositRequestRepository depositRequestRepository;
    private final UserAccountRepository userAccountRepository;
    private final WalletLedgerService walletLedgerService;
    private final AuditService auditService;
    private final Pattern depositCodePattern;

    public AdminBankTxManagerService(BankTransactionRepository bankTransactionRepository,
            DepositRequestRepository depositRequestRepository,
            UserAccountRepository userAccountRepository,
            WalletLedgerService walletLedgerService,
            AuditService auditService,
            BankProperties bankProperties) {
        this.bankTransactionRepository = bankTransactionRepository;
        this.depositRequestRepository = depositRequestRepository;
        this.userAccountRepository = userAccountRepository;
        this.walletLedgerService = walletLedgerService;
        this.auditService = auditService;
        this.depositCodePattern = Pattern.compile("\\b" + Pattern.quote(bankProperties.getTransferPrefix())
                + "[A-Z0-9]{8,}\\b", Pattern.CASE_INSENSITIVE);
    }

    @Transactional(readOnly = true)
    public List<AdminBankTransactionResponse> listBankTransactions(BankTransactionStatus status) {
        return listBankTransactions(status, null, null, 0, 100);
    }

    @Transactional(readOnly = true)
    public List<AdminBankTransactionResponse> listBankTransactions(BankTransactionStatus status,
            Instant fromDate, Instant toDate, int page, int size) {
        List<BankTransaction> transactions = status == null
                ? bankTransactionRepository.findAllByOrderByReceivedAtDesc(paged(page, size))
                : bankTransactionRepository.findAllByStatusOrderByReceivedAtDesc(status, paged(page, size));
        return transactions.stream().map(AdminBankTransactionResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<AdminBankTransactionResponse> listBankTransactions(BankTransactionStatus status, Integer limit) {
        return listBankTransactions(status, null, null, 0, limit == null ? 100 : limit);
    }

    @Transactional(readOnly = true)
    public List<AdminBankTransactionResponse> searchBankTransactions(String query, BankTransactionStatus status,
            Instant fromDate, Instant toDate, int page, int size) {
        String normalizedQuery = normalizeQuery(query);
        Long parsedId = parseLongOrNull(normalizedQuery);
        List<BankTransaction> transactions = bankTransactionRepository.searchAdmin(
                likePattern(normalizedQuery),
                parsedId,
                parsedId,
                status,
                paged(page, size));
        return transactions.stream().map(AdminBankTransactionResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<AdminBankTransactionResponse> searchBankTransactions(String query, BankTransactionStatus status,
            Integer limit) {
        return searchBankTransactions(query, status, null, null, 0, limit == null ? 100 : limit);
    }

    @Transactional(readOnly = true)
    public AdminBankTransactionResponse getBankTransaction(Long bankTransactionId) {
        return bankTransactionRepository.findById(bankTransactionId)
                .map(AdminBankTransactionResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bank transaction not found"));
    }

    @Transactional
    public AdminBankTransactionResponse ignoreBankTransaction(Long adminUserId, Long bankTransactionId,
            IgnoreBankTransactionRequest request) {
        BankTransaction transaction = bankTransactionRepository.findByIdForUpdate(bankTransactionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bank transaction not found"));
        ensureNotCredited(transaction);

        transaction.ignore(request.reason());
        auditService.recordAdmin(
                adminUserId,
                "BANK_TRANSACTION_IGNORED",
                "BANK_TRANSACTION",
                transaction.getId(),
                "reason=" + request.reason());
        return AdminBankTransactionResponse.from(transaction);
    }

    @Transactional
    public AdminBankTransactionResponse matchBankTransaction(Long adminUserId, Long bankTransactionId,
            MatchBankTransactionRequest request) {
        BankTransaction bankTransaction = bankTransactionRepository.findByIdForUpdate(bankTransactionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bank transaction not found"));
        ensureNotCredited(bankTransaction);
        ensureNotDuplicate(bankTransaction);
        ensureIncomingTransfer(bankTransaction);

        DepositRequest deposit = depositRequestRepository.findByDepositCodeForUpdate(
                        normalizeDepositCode(request.depositCode()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Deposit request not found"));
        ensureDepositCanBeLinked(deposit);

        if (deposit.getStatus() == DepositStatus.PENDING && deposit.getExpiredAt().isBefore(Instant.now())) {
            deposit.markManualReview();
        }
        if (deposit.getStatus() == DepositStatus.PENDING
                && !amountMatches(deposit.getAmount(), bankTransaction.getTransferAmount())) {
            deposit.markManualReview();
        }

        bankTransaction.match(deposit, request.reason());
        auditService.recordAdmin(
                adminUserId,
                "BANK_TRANSACTION_MATCHED",
                "BANK_TRANSACTION",
                bankTransaction.getId(),
                "depositCode=" + deposit.getDepositCode() + ",reason=" + request.reason());
        return AdminBankTransactionResponse.from(bankTransaction);
    }

    @Transactional
    public AdminBankTransactionResponse reprocessBankTransaction(Long adminUserId, Long bankTransactionId,
            ReprocessBankTransactionRequest request) {
        BankTransaction bankTransaction = bankTransactionRepository.findByIdForUpdate(bankTransactionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bank transaction not found"));
        ensureNotCredited(bankTransaction);
        ensureNotDuplicate(bankTransaction);

        if (!isIncomingTransfer(bankTransaction)) {
            bankTransaction.ignore("Reprocess ignored non-incoming transfer: " + request.reason());
            auditService.recordAdmin(
                    adminUserId,
                    "BANK_TRANSACTION_REPROCESS_IGNORED",
                    "BANK_TRANSACTION",
                    bankTransaction.getId(),
                    "reason=" + request.reason());
            return AdminBankTransactionResponse.from(bankTransaction);
        }

        BigDecimal transferAmount = bankTransaction.getTransferAmount();
        if (transferAmount == null || transferAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return markManualReviewAfterReprocess(
                    adminUserId,
                    bankTransaction,
                    "Bank transaction amount is invalid",
                    null,
                    request.reason());
        }

        DepositRequest deposit = resolveDepositForReprocess(bankTransaction, request.depositCode()).orElse(null);
        if (deposit == null) {
            return markManualReviewAfterReprocess(
                    adminUserId,
                    bankTransaction,
                    "Deposit request not found",
                    null,
                    request.reason());
        }

        if (deposit.getStatus() == DepositStatus.COMPLETED || deposit.getWalletTransaction() != null) {
            return markManualReviewAfterReprocess(
                    adminUserId,
                    bankTransaction,
                    "Deposit request has already been credited",
                    deposit,
                    request.reason());
        }
        if (deposit.getStatus() == DepositStatus.CANCELLED) {
            return markManualReviewAfterReprocess(
                    adminUserId,
                    bankTransaction,
                    "Deposit request has been cancelled",
                    deposit,
                    request.reason());
        }
        if (deposit.getExpiredAt().isBefore(Instant.now())) {
            deposit.markManualReview();
            return markManualReviewAfterReprocess(
                    adminUserId,
                    bankTransaction,
                    "Deposit request expired",
                    deposit,
                    request.reason());
        }
        if (!amountMatches(deposit.getAmount(), transferAmount)) {
            deposit.markManualReview();
            return markManualReviewAfterReprocess(
                    adminUserId,
                    bankTransaction,
                    "Transfer amount does not match deposit request",
                    deposit,
                    request.reason());
        }

        UserAccount user = userAccountRepository.findByIdForUpdate(deposit.getUser().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (user.getStatus() != UserStatus.ACTIVE) {
            return markManualReviewAfterReprocess(
                    adminUserId,
                    bankTransaction,
                    "User is not active",
                    deposit,
                    request.reason());
        }

        WalletTransaction walletTransaction = walletLedgerService.credit(
                user,
                transferAmount,
                WalletTransactionType.DEPOSIT,
                "DEPOSIT_REQUEST",
                deposit.getId(),
                "Reprocessed bank deposit " + bankTransaction.getId() + " for " + deposit.getDepositCode(),
                adminUserId);
        deposit.complete(bankTransaction, walletTransaction);
        bankTransaction.credit(deposit, walletTransaction);

        auditService.recordAdmin(
                adminUserId,
                "BANK_TRANSACTION_REPROCESSED_CREDITED",
                "BANK_TRANSACTION",
                bankTransaction.getId(),
                "depositCode=" + deposit.getDepositCode() + ",walletTransactionId=" + walletTransaction.getId()
                        + ",reason=" + request.reason());
        return AdminBankTransactionResponse.from(bankTransaction);
    }

    @Transactional
    public AdminBankTransactionResponse manualCreditBankTransaction(Long adminUserId, Long bankTransactionId,
            ManualCreditBankTransactionRequest request) {
        BankTransaction bankTransaction = bankTransactionRepository.findByIdForUpdate(bankTransactionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bank transaction not found"));
        ensureNotCredited(bankTransaction);
        ensureIncomingTransfer(bankTransaction);

        BigDecimal transferAmount = bankTransaction.getTransferAmount();
        if (transferAmount == null || transferAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bank transaction amount is invalid");
        }

        UserAccount user = userAccountRepository.findByIdForUpdate(request.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not active");
        }

        DepositRequest depositRequest = findOptionalDepositForManualCredit(request.depositCode(), user);
        WalletTransaction walletTransaction = walletLedgerService.credit(
                user,
                transferAmount,
                WalletTransactionType.DEPOSIT,
                "BANK_TRANSACTION",
                bankTransaction.getId(),
                "Manual bank credit " + bankTransaction.getId() + ": " + request.reason(),
                adminUserId);

        if (depositRequest != null) {
            depositRequest.complete(bankTransaction, walletTransaction);
        }
        bankTransaction.creditManually(user, depositRequest, walletTransaction, request.reason());

        auditService.recordAdmin(
                adminUserId,
                "BANK_TRANSACTION_MANUAL_CREDITED",
                "BANK_TRANSACTION",
                bankTransaction.getId(),
                "userId=" + user.getId() + ",walletTransactionId=" + walletTransaction.getId()
                        + ",reason=" + request.reason());
        return AdminBankTransactionResponse.from(bankTransaction);
    }

    @Transactional
    public List<AdminBankTransactionResponse> bulkManualCreditBankTransactions(Long adminUserId,
            BulkManualCreditBankTransactionsRequest request) {
        return request.bankTransactionIds().stream()
                .map(id -> manualCreditBankTransaction(adminUserId, id,
                        new ManualCreditBankTransactionRequest(request.userId(), request.depositCode(),
                                request.reason())))
                .toList();
    }

    private DepositRequest findOptionalDepositForManualCredit(String depositCode, UserAccount user) {
        if (depositCode == null || depositCode.isBlank()) {
            return null;
        }

        DepositRequest depositRequest = depositRequestRepository
                .findByDepositCodeForUpdate(depositCode.trim().toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Deposit request not found"));
        if (!depositRequest.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Deposit request belongs to another user");
        }
        if (depositRequest.getStatus() == DepositStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Deposit request has already been completed");
        }
        if (depositRequest.getStatus() == DepositStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Deposit request has been cancelled");
        }
        return depositRequest;
    }

    private Optional<DepositRequest> resolveDepositForReprocess(BankTransaction bankTransaction, String depositCode) {
        if (depositCode != null && !depositCode.isBlank()) {
            return depositRequestRepository.findByDepositCodeForUpdate(normalizeDepositCode(depositCode));
        }

        if (bankTransaction.getMatchedDepositRequest() != null) {
            return depositRequestRepository.findByDepositCodeForUpdate(
                    bankTransaction.getMatchedDepositRequest().getDepositCode());
        }

        return extractDepositCode(bankTransaction.getContent(), bankTransaction.getCode())
                .flatMap(depositRequestRepository::findByDepositCodeForUpdate);
    }

    private AdminBankTransactionResponse markManualReviewAfterReprocess(Long adminUserId,
            BankTransaction bankTransaction, String reviewReason, DepositRequest deposit, String adminReason) {
        bankTransaction.manualReview(reviewReason + ": " + adminReason, deposit);
        auditService.recordAdmin(
                adminUserId,
                "BANK_TRANSACTION_REPROCESS_MANUAL_REVIEW",
                "BANK_TRANSACTION",
                bankTransaction.getId(),
                "reviewReason=" + reviewReason + ",adminReason=" + adminReason
                        + (deposit == null ? "" : ",depositCode=" + deposit.getDepositCode()));
        return AdminBankTransactionResponse.from(bankTransaction);
    }

    private void ensureNotCredited(BankTransaction transaction) {
        if (transaction.getStatus() == BankTransactionStatus.CREDITED || transaction.getWalletTransaction() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bank transaction has already been credited");
        }
    }

    private void ensureNotDuplicate(BankTransaction transaction) {
        if (transaction.getStatus() == BankTransactionStatus.DUPLICATE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Duplicate bank transaction cannot be reprocessed");
        }
    }

    private void ensureIncomingTransfer(BankTransaction transaction) {
        if (!isIncomingTransfer(transaction)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only incoming bank transactions can be credited");
        }
    }

    private boolean isIncomingTransfer(BankTransaction transaction) {
        return transaction.getTransferType() == null || TRANSFER_IN.equalsIgnoreCase(transaction.getTransferType());
    }

    private void ensureDepositCanBeLinked(DepositRequest deposit) {
        if (deposit.getStatus() == DepositStatus.COMPLETED || deposit.getWalletTransaction() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Deposit request has already been credited");
        }
    }

    private boolean amountMatches(BigDecimal expectedAmount, BigDecimal actualAmount) {
        return expectedAmount != null && actualAmount != null && expectedAmount.compareTo(actualAmount) == 0;
    }

    private Optional<String> extractDepositCode(String content, String code) {
        String text = (nullSafe(content) + " " + nullSafe(code)).toUpperCase(Locale.ROOT);
        Matcher matcher = depositCodePattern.matcher(text);
        if (matcher.find()) {
            return Optional.of(matcher.group().toUpperCase(Locale.ROOT));
        }
        return Optional.empty();
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

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
