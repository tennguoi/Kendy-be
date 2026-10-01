package com.example.KendyDigital.service.security.session;

import com.example.KendyDigital.dto.auth.response.AuthSessionResponse;
import java.util.List;

public interface UserSessionService {
    List<AuthSessionResponse> sessions(Long userId, int page, int size);
    void revokeSession(Long userId, Long sessionId);
    void revokeAllSessions(Long userId);
    long countActiveSessions(Long userId);
}
