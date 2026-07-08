package com.example.KendyDigital.dto.order.request;

public record ManualOrderTaskRequest(
        Long id,
        String title,
        Boolean completed,
        Integer sortOrder) {
}
