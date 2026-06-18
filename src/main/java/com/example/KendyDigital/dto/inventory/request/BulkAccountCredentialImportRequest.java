package com.example.KendyDigital.dto.inventory.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import java.util.List;

public record BulkAccountCredentialImportRequest(
        String csvContent,
        @Valid List<CreateAccountCredentialRequest> credentials,
        Boolean skipDuplicates) {
    @AssertTrue(message = "csvContent or credentials must be provided")
    public boolean hasImportData() {
        return (csvContent != null && !csvContent.isBlank())
                || (credentials != null && !credentials.isEmpty());
    }
}
