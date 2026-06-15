package com.example.KendyDigital.dto.catalog.request;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record IdsRequest(
        @NotEmpty List<Long> ids,
        String reason) {
}
