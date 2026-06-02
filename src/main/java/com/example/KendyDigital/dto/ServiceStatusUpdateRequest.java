package com.example.KendyDigital.dto;

import com.example.KendyDigital.model.ServiceStatus;

import jakarta.validation.constraints.NotNull;

public record ServiceStatusUpdateRequest(
        @NotNull ServiceStatus status) {
}
