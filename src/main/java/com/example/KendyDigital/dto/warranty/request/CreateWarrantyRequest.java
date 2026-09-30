package com.example.KendyDigital.dto.warranty.request;


import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;

public record CreateWarrantyRequest(
        @NotBlank @Size(max = 1000) String reason,
        @Size(max = 5000) String evidenceText) {
}
