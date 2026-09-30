package com.example.KendyDigital.dto.inventory.request;


import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import java.util.List;

public record BulkAccountCredentialImportRequest(
        @Size(max = 2000000) String csvContent,
        @Valid List<CreateAccountCredentialRequest> credentials,
        Boolean skipDuplicates) {
    @AssertTrue(message = "csvContent or credentials must be provided")
    public boolean hasImportData() {
        return (csvContent != null && !csvContent.isBlank())
                || (credentials != null && !credentials.isEmpty());
    }
}
