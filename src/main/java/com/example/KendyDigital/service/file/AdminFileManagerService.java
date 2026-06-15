package com.example.KendyDigital.service.file;

import com.example.KendyDigital.dto.file.response.StoredFileResponse;
import com.example.KendyDigital.model.file.StoredFile;
import org.springframework.web.multipart.MultipartFile;

public interface AdminFileManagerService {
    StoredFileResponse uploadFile(Long adminUserId, MultipartFile file);
    StoredFile getFile(Long fileId);
    void deleteFile(Long adminUserId, Long fileId);
    byte[] previewBytes(StoredFile file);
}
