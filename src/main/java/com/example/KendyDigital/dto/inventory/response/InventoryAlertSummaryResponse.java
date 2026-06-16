package com.example.KendyDigital.dto.inventory.response;

public record InventoryAlertSummaryResponse(
        long lowStockServices,
        long expiringCredentials,
        long reserveReleased) {
}
