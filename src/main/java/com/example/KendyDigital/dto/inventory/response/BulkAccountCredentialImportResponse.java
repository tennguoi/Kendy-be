package com.example.KendyDigital.dto.inventory.response;

import java.util.List;

public record BulkAccountCredentialImportResponse(
        int created,
        int skipped,
        List<String> errors,
        List<AccountCredentialAdminResponse> credentials) {
}
