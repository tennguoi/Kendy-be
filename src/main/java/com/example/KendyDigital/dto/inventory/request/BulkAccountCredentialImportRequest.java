package com.example.KendyDigital.dto.inventory.request;

import jakarta.validation.Valid;
import java.util.List;

public record BulkAccountCredentialImportRequest(
        String csvContent,
        @Valid List<CreateAccountCredentialRequest> credentials,
        Boolean skipDuplicates) {
}
