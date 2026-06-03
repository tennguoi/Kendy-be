package com.example.KendyDigital.dto;

import jakarta.validation.constraints.NotBlank;

public record OrderNoteRequest(
        @NotBlank String note) {
}
