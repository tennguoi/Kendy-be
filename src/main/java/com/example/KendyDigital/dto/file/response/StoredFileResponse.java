package com.example.KendyDigital.dto.file.response;

import com.example.KendyDigital.model.file.StoredFile;
import java.time.Instant;

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
