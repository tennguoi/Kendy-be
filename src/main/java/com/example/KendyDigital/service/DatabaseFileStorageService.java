package com.example.KendyDigital.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.example.KendyDigital.model.StoredFile;
import com.example.KendyDigital.repository.StoredFileRepository;

@Service
public class DatabaseFileStorageService implements FileStorageService {
    private final StoredFileRepository storedFileRepository;

    public DatabaseFileStorageService(StoredFileRepository storedFileRepository) {
        this.storedFileRepository = storedFileRepository;
    }

    @Override
    public StoredFile store(String fileName, String contentType, long sizeBytes, Long uploadedBy, byte[] content) {
        return storedFileRepository.save(new StoredFile(fileName, contentType, sizeBytes, uploadedBy, content));
    }

    @Override
    public StoredFile store(MultipartFile file, Long uploadedBy) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is required");
        }
        try {
            return store(
                    file.getOriginalFilename() == null ? "upload.bin" : file.getOriginalFilename(),
                    file.getContentType(),
                    file.getSize(),
                    uploadedBy,
                    file.getBytes());
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot read uploaded file");
        }
    }

    @Override
    public StoredFile getById(Long id) {
        return storedFileRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));
    }

    @Override
    public byte[] getContent(Long id) {
        return getById(id).getContent();
    }

    @Override
    public void delete(Long id) {
        StoredFile file = getById(id);
        storedFileRepository.delete(file);
    }

    @Override
    public byte[] previewBytes(StoredFile file) {
        if (file.getContentType() != null && file.getContentType().startsWith("text/")) {
            String content = new String(file.getContent(), StandardCharsets.UTF_8);
            return content.substring(0, Math.min(content.length(), 4000)).getBytes(StandardCharsets.UTF_8);
        }
        return file.getContent();
    }

    @Override
    public StoredFileResponse from(StoredFile file) {
        return StoredFileResponse.from(file);
    }
}
