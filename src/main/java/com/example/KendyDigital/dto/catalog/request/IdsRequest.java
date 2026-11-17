package com.example.KendyDigital.dto.catalog.request;


import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

public record IdsRequest(
        @NotEmpty List<UUID> ids,
        @Size(max = 500) String reason) {
}
