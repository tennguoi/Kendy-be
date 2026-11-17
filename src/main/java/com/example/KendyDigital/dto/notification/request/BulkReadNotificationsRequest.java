package com.example.KendyDigital.dto.notification.request;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

public record BulkReadNotificationsRequest(
        @NotEmpty List<UUID> ids) {
}
