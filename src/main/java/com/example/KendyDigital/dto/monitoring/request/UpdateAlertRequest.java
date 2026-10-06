package com.example.KendyDigital.dto.monitoring.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateAlertRequest(
        @NotBlank String status,
        @Size(max = 255) String assignee,
        @Size(max = 2000) String note) {
}
