package com.example.KendyDigital.dto.notification.request;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record BulkReadNotificationsRequest(
        @NotEmpty List<Long> ids) {
}
