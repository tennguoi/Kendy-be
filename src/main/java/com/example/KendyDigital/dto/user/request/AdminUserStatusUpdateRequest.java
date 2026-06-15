package com.example.KendyDigital.dto.user.request;

import com.example.KendyDigital.model.user.UserStatus;
import jakarta.validation.constraints.NotNull;

public record AdminUserStatusUpdateRequest(
        @NotNull UserStatus status,
        String reason) {
}
