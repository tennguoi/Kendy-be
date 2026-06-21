package com.example.KendyDigital.service.catalog;

import com.example.KendyDigital.dto.catalog.response.ServiceImageUploadResponse;
import org.springframework.web.multipart.MultipartFile;

public interface ServiceImageService {
    ServiceImageUploadResponse upload(MultipartFile file);
}
