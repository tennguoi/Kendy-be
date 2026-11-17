package com.example.KendyDigital.service.file_integrations;

import com.example.KendyDigital.dto.file.response.StoredFileResponse;
import com.example.KendyDigital.model.file.StoredFile;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    StoredFile store(String fileName, String contentType, long sizeBytes, Long uploadedBy, byte[] content);

    StoredFile store(MultipartFile file, Long uploadedBy);

    StoredFile getById(Long id);

    byte[] getContent(Long id);

    void delete(Long id);

    byte[] previewBytes(StoredFile file);

    StoredFileResponse from(StoredFile file);

    record StoredFileResponse(
            UUID id,
            String fileName,
            String contentType,
            long sizeBytes,
            Long uploadedBy,
            java.time.Instant createdAt) {
        public static StoredFileResponse from(StoredFile file) {
            return new StoredFileResponse(
                    file.getPublicId(),
                    file.getFileName(),
                    file.getContentType(),
                    file.getSizeBytes(),
                    file.getUploadedBy(),
                    file.getCreatedAt());
        }
    }
}
