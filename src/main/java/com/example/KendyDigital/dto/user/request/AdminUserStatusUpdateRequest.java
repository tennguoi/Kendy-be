package com.example.KendyDigital.dto.user.request;


import jakarta.validation.constraints.Size;
import com.example.KendyDigital.model.user.UserStatus;
import jakarta.validation.constraints.NotNull;

public record AdminUserStatusUpdateRequest(
        @NotNull UserStatus status,
        @Size(max = 500) String reason) {
}
