package com.example.KendyDigital.service.catalog;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.KendyDigital.dto.catalog.response.ServiceImageUploadResponse;
import com.example.KendyDigital.service.file.UploadedFileValidator;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ServiceImageServiceImpl implements ServiceImageService {
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp");

    private final Cloudinary cloudinary;
    private final UploadedFileValidator uploadedFileValidator;
    private final String folder;

    public ServiceImageServiceImpl(
            Cloudinary cloudinary,
            UploadedFileValidator uploadedFileValidator,
            @Value("${app.cloudinary.service-image-folder:kendy-digital/services}") String folder) {
        this.cloudinary = cloudinary;
        this.uploadedFileValidator = uploadedFileValidator;
        this.folder = folder;
    }

    @Override
    public ServiceImageUploadResponse upload(MultipartFile file) {
        UploadedFileValidator.ValidatedUpload upload = uploadedFileValidator.validate(file);
        if (!ALLOWED_IMAGE_TYPES.contains(upload.contentType())) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Only JPG, PNG, GIF and WEBP images are allowed");
        }

        try {
            Map<?, ?> result = cloudinary.uploader().upload(upload.content(), ObjectUtils.asMap(
                    "folder", folder,
                    "resource_type", "image",
                    "use_filename", true,
                    "unique_filename", true,
                    "overwrite", false));
            return new ServiceImageUploadResponse(
                    requiredString(result, "secure_url"),
                    requiredString(result, "public_id"),
                    stringValue(result.get("format")),
                    longValue(result.get("bytes")),
                    intValue(result.get("width")),
                    intValue(result.get("height")));
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Cloudinary image upload failed", exception);
        }
    }

    private String requiredString(Map<?, ?> result, String key) {
        String value = stringValue(result.get(key));
        if (value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Cloudinary response is missing " + key);
        }
        return value;
    }

    private String stringValue(Object value) {
        return value == null ? "" : value.toString();
    }

    private long longValue(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private int intValue(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }
}
