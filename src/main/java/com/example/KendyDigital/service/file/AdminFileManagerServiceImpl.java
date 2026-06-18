package com.example.KendyDigital.service.file;

import com.example.KendyDigital.dto.file.response.StoredFileResponse;
import com.example.KendyDigital.model.file.StoredFile;
import com.example.KendyDigital.repository.StoredFileRepository;
import com.example.KendyDigital.service.audit.AuditService;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminFileManagerServiceImpl  implements AdminFileManagerService{
    private final StoredFileRepository storedFileRepository;
    private final AuditService auditService;
    private final UploadedFileValidator uploadedFileValidator;

    public AdminFileManagerServiceImpl(StoredFileRepository storedFileRepository, AuditService auditService,
            UploadedFileValidator uploadedFileValidator) {
        this.storedFileRepository = storedFileRepository;
        this.auditService = auditService;
        this.uploadedFileValidator = uploadedFileValidator;
    }

    @Transactional
    public StoredFileResponse uploadFile(Long adminUserId, MultipartFile file) {
        UploadedFileValidator.ValidatedUpload upload = uploadedFileValidator.validate(file);
        StoredFile storedFile = storedFileRepository.save(new StoredFile(
                upload.fileName(), upload.contentType(), upload.sizeBytes(), adminUserId, upload.content()));
        auditService.recordAdmin(adminUserId, "FILE_UPLOADED", "FILE", storedFile.getId(),
                "fileName=" + storedFile.getFileName());
        return StoredFileResponse.from(storedFile);
    }

    @Transactional(readOnly = true)
    public StoredFile getFile(Long fileId) {
        return storedFileRepository.findById(fileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));
    }

    @Transactional
    public void deleteFile(Long adminUserId, Long fileId) {
        StoredFile file = getFile(fileId);
        storedFileRepository.delete(file);
        auditService.recordAdmin(adminUserId, "FILE_DELETED", "FILE", fileId, "fileName=" + file.getFileName());
    }

    public byte[] previewBytes(StoredFile file) {
        if (file.getContentType() != null && file.getContentType().startsWith("text/")) {
            String content = new String(file.getContent(), StandardCharsets.UTF_8);
            return content.substring(0, Math.min(content.length(), 4000)).getBytes(StandardCharsets.UTF_8);
        }
        return file.getContent();
    }
}
