package com.example.KendyDigital.dto.order.request;

import jakarta.validation.constraints.NotBlank;

public record OrderNoteRequest(
        @NotBlank String note) {
}
