package com.example.KendyDigital.service.file;

import com.example.KendyDigital.dto.file.response.StoredFileResponse;
import com.example.KendyDigital.model.file.StoredFile;
import com.example.KendyDigital.repository.StoredFileRepository;
import com.example.KendyDigital.service.file.FileStorageService;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DatabaseFileStorageService implements FileStorageService {
    private final StoredFileRepository storedFileRepository;
    private final UploadedFileValidator uploadedFileValidator;

    public DatabaseFileStorageService(StoredFileRepository storedFileRepository,
            UploadedFileValidator uploadedFileValidator) {
        this.storedFileRepository = storedFileRepository;
        this.uploadedFileValidator = uploadedFileValidator;
    }

    @Override
    @Transactional
    public StoredFile store(String fileName, String contentType, long sizeBytes, Long uploadedBy, byte[] content) {
        return storedFileRepository.save(new StoredFile(fileName, contentType, sizeBytes, uploadedBy, content));
    }

    @Override
    @Transactional
    public StoredFile store(MultipartFile file, Long uploadedBy) {
        UploadedFileValidator.ValidatedUpload upload = uploadedFileValidator.validate(file);
        return store(upload.fileName(), upload.contentType(), upload.sizeBytes(), uploadedBy, upload.content());
    }

    @Override
    @Transactional(readOnly = true)
    public StoredFile getById(Long id) {
        return storedFileRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] getContent(Long id) {
        return getById(id).getContent();
    }

    @Override
    @Transactional
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
