package com.example.KendyDigital.service.user_management.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.KendyDigital.service.file_integrations.UploadedFileValidator;
import com.example.KendyDigital.service.user_management.UserAvatarService;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserAvatarServiceImpl implements UserAvatarService {
    private static final Set<String> AVATAR_CONTENT_TYPES =
            Set.of("image/jpeg", "image/png", "image/gif", "image/webp");

    private final Cloudinary cloudinary;
    private final UploadedFileValidator uploadedFileValidator;

    public UserAvatarServiceImpl(Cloudinary cloudinary, UploadedFileValidator uploadedFileValidator) {
        this.cloudinary = cloudinary;
        this.uploadedFileValidator = uploadedFileValidator;
    }

    @Override
    public String uploadAvatar(Long userId, String userPublicId, MultipartFile file) {
        UploadedFileValidator.ValidatedUpload upload = uploadedFileValidator.validate(file);
        if (!AVATAR_CONTENT_TYPES.contains(upload.contentType())) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Only JPG, PNG, GIF and WEBP images are allowed");
        }

        try {
            Map<?, ?> result = cloudinary.uploader().upload(upload.content(), ObjectUtils.asMap(
                    "folder", "kendy-digital/avatars",
                    "public_id", "user-" + userPublicId,
                    "resource_type", "image",
                    "overwrite", true,
                    "invalidate", true));
            Object secureUrl = result.get("secure_url");
            if (secureUrl == null || secureUrl.toString().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "Cloudinary response is missing secure_url");
            }
            return secureUrl.toString();
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Cloudinary avatar upload failed", exception);
        }
    }
}
