package com.example.KendyDigital.dto.catalog.request;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;

public record IdsRequest(
        @NotEmpty List<Long> ids,
        String reason) {
}
