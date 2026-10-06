package com.example.KendyDigital.dto.monitoring.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BanIpRequest(
        @NotBlank @Size(max = 64) String ipOrCidr,
        @Size(max = 255) String reason,
        Integer durationMinutes) {
}
