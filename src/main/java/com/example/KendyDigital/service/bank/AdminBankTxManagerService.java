package com.example.KendyDigital.service.bank;

import com.example.KendyDigital.dto.finance.request.BulkManualCreditBankTransactionsRequest;
import com.example.KendyDigital.dto.finance.request.IgnoreBankTransactionRequest;
import com.example.KendyDigital.dto.finance.request.ManualCreditBankTransactionRequest;
import com.example.KendyDigital.dto.finance.request.MatchBankTransactionRequest;
import com.example.KendyDigital.dto.finance.request.ReprocessBankTransactionRequest;
import com.example.KendyDigital.dto.finance.response.AdminBankTransactionResponse;
import com.example.KendyDigital.model.bank.BankTransactionStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AdminBankTxManagerService {
    List<AdminBankTransactionResponse> listBankTransactions(BankTransactionStatus status);
    List<AdminBankTransactionResponse> listBankTransactions(BankTransactionStatus status, Instant fromDate, Instant toDate, int page, int size);
    List<AdminBankTransactionResponse> listBankTransactions(BankTransactionStatus status, Integer limit);
    List<AdminBankTransactionResponse> searchBankTransactions(String query, BankTransactionStatus status, Instant fromDate, Instant toDate, int page, int size);
    List<AdminBankTransactionResponse> searchBankTransactions(String query, BankTransactionStatus status, Integer limit);
    AdminBankTransactionResponse getBankTransaction(UUID bankTransactionId);
    AdminBankTransactionResponse ignoreBankTransaction(Long adminUserId, UUID bankTransactionId, IgnoreBankTransactionRequest request);
    AdminBankTransactionResponse matchBankTransaction(Long adminUserId, UUID bankTransactionId, MatchBankTransactionRequest request);
    AdminBankTransactionResponse reprocessBankTransaction(Long adminUserId, UUID bankTransactionId, ReprocessBankTransactionRequest request);
    AdminBankTransactionResponse manualCreditBankTransaction(Long adminUserId, UUID bankTransactionId, ManualCreditBankTransactionRequest request);
    List<AdminBankTransactionResponse> bulkManualCreditBankTransactions(Long adminUserId, BulkManualCreditBankTransactionsRequest request);
}
