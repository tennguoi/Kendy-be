package com.example.KendyDigital.controller;

import com.example.KendyDigital.dto.catalog.response.ServiceImageUploadResponse;
import com.example.KendyDigital.service.catalog.ServiceImageService;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class AdminServiceImageController {
    private final ServiceImageService serviceImageService;

    public AdminServiceImageController(ServiceImageService serviceImageService) {
        this.serviceImageService = serviceImageService;
    }

    @PostMapping(value = "/api/admin/services/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ServiceImageUploadResponse upload(@RequestPart("file") MultipartFile file) {
        return serviceImageService.upload(file);
    }
}
