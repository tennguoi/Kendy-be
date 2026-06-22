package com.example.KendyDigital.dto.entitlement.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AdminEntitlementUpdateRequest(
        @NotNull Action action,
        @Min(1) Integer extendDays,
        String accessIdentifier,
        String externalResourceId,
        String providerMetadata,
        String reason) {

    public enum Action {
        ACTIVATE,
        SUSPEND,
        REVOKE,
        EXTEND
    }
}
