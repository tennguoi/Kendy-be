package com.example.KendyDigital.dto.catalog.response;

public record ServiceImageUploadResponse(
        String url,
        String publicId,
        String format,
        long bytes,
        int width,
        int height) {
}
