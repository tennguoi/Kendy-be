package com.example.KendyDigital.dto;

import java.time.Instant;

import com.example.KendyDigital.model.StoredFile;

public record StoredFileResponse(
        Long id,
        String fileName,
        String contentType,
        long sizeBytes,
        Long uploadedBy,
        Instant createdAt) {
    public static StoredFileResponse from(StoredFile file) {
        return new StoredFileResponse(
                file.getId(),
                file.getFileName(),
                file.getContentType(),
                file.getSizeBytes(),
                file.getUploadedBy(),
                file.getCreatedAt());
    }
}
