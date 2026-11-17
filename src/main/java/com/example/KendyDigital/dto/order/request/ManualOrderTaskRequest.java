package com.example.KendyDigital.dto.order.request;


import jakarta.validation.constraints.Size;
import java.util.UUID;
public record ManualOrderTaskRequest(
        UUID id,
        @Size(max = 200) String title,
        Boolean completed,
        Integer sortOrder) {
}
