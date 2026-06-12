package com.example.KendyDigital.dto.notification.request;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;

public record BulkReadNotificationsRequest(
        @NotEmpty List<Long> ids) {
}
