package com.example.KendyDigital.service.security.session;

import com.example.KendyDigital.dto.auth.response.AuthSessionResponse;
import java.util.List;
import java.util.UUID;

public interface UserSessionService {
    List<AuthSessionResponse> sessions(Long userId, int page, int size);
    void revokeSession(Long userId, UUID sessionId);
    void revokeAllSessions(Long userId);
    long countActiveSessions(Long userId);
}
