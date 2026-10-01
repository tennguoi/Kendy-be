package com.example.KendyDigital.service.user_management;

import org.springframework.web.multipart.MultipartFile;

public interface UserAvatarService {
    String uploadAvatar(Long userId, String userPublicId, MultipartFile file);
}
