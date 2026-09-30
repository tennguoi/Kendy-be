package com.example.KendyDigital.dto.entitlement.request;


import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AdminEntitlementUpdateRequest(
        @NotNull Action action,
        @Min(1) Integer extendDays,
        @Size(max = 150) String accessIdentifier,
        @Size(max = 150) String externalResourceId,
        @Size(max = 10000) String providerMetadata,
        @Size(max = 500) String reason) {

    public enum Action {
        ACTIVATE,
        SUSPEND,
        REVOKE,
        EXTEND
    }
}
