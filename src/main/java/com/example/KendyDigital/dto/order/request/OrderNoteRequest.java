package com.example.KendyDigital.dto.order.request;


import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;

public record OrderNoteRequest(
        @NotBlank @Size(max = 1000) String note) {
}
