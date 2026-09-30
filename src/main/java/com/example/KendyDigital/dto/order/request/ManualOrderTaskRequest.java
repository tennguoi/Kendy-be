package com.example.KendyDigital.dto.order.request;


import jakarta.validation.constraints.Size;
public record ManualOrderTaskRequest(
        Long id,
        @Size(max = 200) String title,
        Boolean completed,
        Integer sortOrder) {
}
