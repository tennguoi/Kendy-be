package com.example.KendyDigital.controller;

import com.example.KendyDigital.dto.file.response.StoredFileResponse;
import com.example.KendyDigital.model.file.StoredFile;
import com.example.KendyDigital.security.CurrentUser;
import com.example.KendyDigital.service.file.AdminFileManagerService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class AdminFileController {
    private final AdminFileManagerService fileManagerService;

    public AdminFileController(AdminFileManagerService fileManagerService) {
        this.fileManagerService = fileManagerService;
    }

    @PostMapping(value = "/api/admin/files/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public StoredFileResponse uploadFile(Authentication authentication, @RequestPart("file") MultipartFile file) {
        return fileManagerService.uploadFile(CurrentUser.require(authentication).userId(), file);
    }

    @GetMapping("/api/admin/files/{fileId}/download")
    public ResponseEntity<byte[]> downloadFile(@PathVariable Long fileId) {
        StoredFile file = fileManagerService.getFile(fileId);
        return fileResponse(file, file.getContent());
    }

    @DeleteMapping("/api/admin/files/{fileId}/delete")
    public void deleteFile(Authentication authentication, @PathVariable Long fileId) {
        fileManagerService.deleteFile(CurrentUser.require(authentication).userId(), fileId);
    }

    @GetMapping("/api/admin/files/{fileId}/preview")
    public ResponseEntity<byte[]> previewFile(@PathVariable Long fileId) {
        StoredFile file = fileManagerService.getFile(fileId);
        return fileResponse(file, fileManagerService.previewBytes(file));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ResponseEntity<byte[]> fileResponse(StoredFile file, byte[] content) {
        MediaType mediaType = file.getContentType() == null
                ? MediaType.APPLICATION_OCTET_STREAM
                : MediaType.parseMediaType(file.getContentType());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(file.getFileName()).build().toString())
                .contentType(mediaType)
                .body(content);
    }
}
