package com.example.KendyDigital.service.webhook;

import com.example.KendyDigital.config.BankProperties;
import com.example.KendyDigital.dto.webhook.request.SePayWebhookPayload;
import com.example.KendyDigital.model.bank.BankTransaction;
import com.example.KendyDigital.model.deposit.DepositRequest;
import com.example.KendyDigital.model.deposit.DepositStatus;
import com.example.KendyDigital.model.user.UserAccount;
import com.example.KendyDigital.model.wallet.WalletTransaction;
import com.example.KendyDigital.model.wallet.WalletTransactionType;
import com.example.KendyDigital.repository.*;
import com.example.KendyDigital.repository.UserAccountRepository;
import com.example.KendyDigital.service.audit.AuditService;
import com.example.KendyDigital.service.checkout.CheckoutService;
import com.example.KendyDigital.service.notification.UserNotificationService;
import com.example.KendyDigital.service.wallet.WalletLedgerService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SePayWebhookServiceImpl  implements SePayWebhookService{
    private static final String TRANSFER_IN = "IN";

    private final BankTransactionInserter bankTransactionInserter;
    private final BankTransactionRepository bankTransactionRepository;
    private final DepositRequestRepository depositRequestRepository;
    private final UserAccountRepository userAccountRepository;
    private final WalletLedgerService walletLedgerService;
    private final AuditService auditService;
    private final UserNotificationService userNotificationService;
    private final CheckoutService checkoutService;
    private final Pattern depositCodePattern;

    public SePayWebhookServiceImpl(BankTransactionInserter bankTransactionInserter,
            BankTransactionRepository bankTransactionRepository,
            DepositRequestRepository depositRequestRepository,
            UserAccountRepository userAccountRepository,
            WalletLedgerService walletLedgerService,
            AuditService auditService,
            UserNotificationService userNotificationService,
            CheckoutService checkoutService,
            BankProperties bankProperties) {
        this.bankTransactionInserter = bankTransactionInserter;
        this.bankTransactionRepository = bankTransactionRepository;
        this.depositRequestRepository = depositRequestRepository;
        this.userAccountRepository = userAccountRepository;
        this.walletLedgerService = walletLedgerService;
        this.auditService = auditService;
        this.userNotificationService = userNotificationService;
        this.checkoutService = checkoutService;
        this.depositCodePattern = Pattern.compile("\\b" + Pattern.quote(bankProperties.getTransferPrefix())
                + "[A-Z0-9]{8,}\\b", Pattern.CASE_INSENSITIVE);
    }

    @Transactional
    public void process(SePayWebhookPayload payload, String rawPayload) {
        validatePayload(payload);
        Instant receivedAt = Instant.now();
        Optional<Long> insertedId = bankTransactionInserter.insertNew(
                payload,
                rawPayload,
                receivedAt,
                parseTransactionDate(payload.transactionDate()));

        if (insertedId.isEmpty()) {
            return;
        }

        BankTransaction bankTransaction = bankTransactionRepository.findById(insertedId.get())
                .orElseThrow(() -> new IllegalStateException("Inserted bank transaction not found"));

        if (!TRANSFER_IN.equalsIgnoreCase(nullSafe(payload.transferType()))) {
            bankTransaction.ignore("Only incoming transfer is eligible for wallet credit");
            return;
        }

        Optional<String> depositCode = extractDepositCode(payload.content(), payload.code());
        if (depositCode.isEmpty()) {
            bankTransaction.manualReview("Deposit code not found in transfer content", null);
            return;
        }

        DepositRequest deposit = depositRequestRepository.findByDepositCodeForUpdate(depositCode.get().toUpperCase(Locale.ROOT))
                .orElse(null);
        if (deposit == null) {
            bankTransaction.manualReview("Deposit request not found", null);
            return;
        }

        if (deposit.getStatus() != DepositStatus.PENDING) {
            bankTransaction.manualReview("Deposit request is not pending", deposit);
            return;
        }

        if (deposit.getExpiredAt().isBefore(Instant.now())) {
            deposit.markManualReview();
            bankTransaction.manualReview("Deposit request expired", deposit);
            return;
        }

        BigDecimal transferAmount = normalizeAmount(payload.transferAmount());
        if (deposit.getAmount().compareTo(transferAmount) != 0) {
            deposit.markManualReview();
            bankTransaction.manualReview("Transfer amount does not match deposit request", deposit);
            return;
        }

        UserAccount user = userAccountRepository.findByIdForUpdate(deposit.getUser().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        WalletTransaction walletTransaction = walletLedgerService.credit(
                user,
                transferAmount,
                WalletTransactionType.DEPOSIT,
                "DEPOSIT_REQUEST",
                deposit.getId(),
                "SePay deposit " + deposit.getDepositCode(),
                null);

        deposit.complete(bankTransaction, walletTransaction);
        bankTransaction.credit(deposit, walletTransaction);
        checkoutService.completePaidDeposit(deposit.getId());
        auditService.recordSystem(
                "SEPAY_DEPOSIT_CREDITED",
                "DEPOSIT_REQUEST",
                deposit.getId(),
                "bankTransactionId=" + bankTransaction.getId() + ",walletTransactionId=" + walletTransaction.getId());
        userNotificationService.create(user.getId(),
                "Deposit completed",
                "Deposit " + deposit.getDepositCode() + " has been credited to your wallet.",
                "DEPOSIT",
                "/deposits/" + deposit.getDepositCode());
    }

    private void validatePayload(SePayWebhookPayload payload) {
        if (payload.transferAmount() == null || payload.transferAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "transferAmount must be greater than zero");
        }
        if (isBlank(payload.referenceCode()) && payload.id() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "referenceCode or id is required");
        }
    }

    private Optional<String> extractDepositCode(String content, String code) {
        String text = (nullSafe(content) + " " + nullSafe(code)).toUpperCase(Locale.ROOT);
        Matcher matcher = depositCodePattern.matcher(text);
        if (matcher.find()) {
            return Optional.of(matcher.group().toUpperCase(Locale.ROOT));
        }
        return Optional.empty();
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        try {
            return amount.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "transferAmount must have at most two decimal places");
        }
    }

    private Instant parseTransactionDate(String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                        .atZone(ZoneId.systemDefault())
                        .toInstant();
            } catch (DateTimeParseException ignoredAgain) {
                return null;
            }
        }
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
