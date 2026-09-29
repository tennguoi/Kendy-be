package com.example.KendyDigital.dto.user.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Used by OAuth-only accounts to set a password for the first time.
 * Unlike ChangePasswordRequest, this does NOT require currentPassword
 * because the user never had one they knew.
 */
public record SetPasswordRequest(
        @NotBlank @Size(min = 8) String newPassword) {
}
