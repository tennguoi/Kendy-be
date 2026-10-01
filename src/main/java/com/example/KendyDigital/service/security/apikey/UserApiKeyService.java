package com.example.KendyDigital.service.security.apikey;

import com.example.KendyDigital.dto.user.request.UserApiKeyCreateRequest;
import com.example.KendyDigital.dto.user.response.UserApiKeyCreatedResponse;
import com.example.KendyDigital.dto.user.response.UserApiKeyResponse;
import com.example.KendyDigital.security.ResolvedApiKey;
import java.util.List;
import java.util.Optional;

public interface UserApiKeyService {
    List<UserApiKeyResponse> apiKeys(Long userId, int page, int size);
    UserApiKeyCreatedResponse createApiKey(Long userId, UserApiKeyCreateRequest request);
    void revokeApiKey(Long userId, Long keyId);
    void revokeAllApiKeys(Long userId);
    Optional<ResolvedApiKey> resolveApiKey(String token);
    long countActiveKeys(Long userId);
}
