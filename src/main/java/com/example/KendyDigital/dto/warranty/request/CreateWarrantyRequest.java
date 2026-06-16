package com.example.KendyDigital.dto.warranty.request;

import jakarta.validation.constraints.NotBlank;

public record CreateWarrantyRequest(
        @NotBlank String reason,
        String evidenceText) {
}
