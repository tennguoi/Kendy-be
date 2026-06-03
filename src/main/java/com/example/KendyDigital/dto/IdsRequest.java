package com.example.KendyDigital.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;

public record IdsRequest(
        @NotEmpty List<Long> ids,
        String reason) {
}
