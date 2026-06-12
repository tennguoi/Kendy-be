package com.example.KendyDigital.dto.catalog.request;

import com.example.KendyDigital.model.ServiceStatus;

import jakarta.validation.constraints.NotNull;

public record ServiceStatusUpdateRequest(
        @NotNull ServiceStatus status) {
}
