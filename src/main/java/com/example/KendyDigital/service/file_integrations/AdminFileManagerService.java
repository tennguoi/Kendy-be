package com.example.KendyDigital.service.file_integrations;

import com.example.KendyDigital.dto.file.response.StoredFileResponse;
import com.example.KendyDigital.model.file.StoredFile;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public interface AdminFileManagerService {
    StoredFileResponse uploadFile(Long adminUserId, MultipartFile file);
    StoredFile getFile(UUID fileId);
    void deleteFile(Long adminUserId, UUID fileId);
    byte[] previewBytes(StoredFile file);
}
