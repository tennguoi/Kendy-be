package com.example.KendyDigital.service.security.session;

import com.example.KendyDigital.dto.auth.response.AuthSessionResponse;
import com.example.KendyDigital.model.auth.AuthSession;
import com.example.KendyDigital.model.user.UserAccount;
import com.example.KendyDigital.repository.AuthSessionRepository;
import com.example.KendyDigital.repository.UserAccountRepository;
import com.example.KendyDigital.service.audit.AuditService;
import com.example.KendyDigital.service.notification.EmailNotificationService;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserSessionServiceImpl implements UserSessionService {
    private final AuthSessionRepository authSessionRepository;
    private final UserAccountRepository userAccountRepository;
    private final AuditService auditService;
    private final EmailNotificationService emailNotificationService;

    public UserSessionServiceImpl(
            AuthSessionRepository authSessionRepository,
            UserAccountRepository userAccountRepository,
            AuditService auditService,
            EmailNotificationService emailNotificationService) {
        this.authSessionRepository = authSessionRepository;
        this.userAccountRepository = userAccountRepository;
        this.auditService = auditService;
        this.emailNotificationService = emailNotificationService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuthSessionResponse> sessions(Long userId, int page, int size) {
        return authSessionRepository.findAllByUser_IdOrderByCreatedAtDesc(userId, paged(page, size))
                .stream()
                .map(AuthSessionResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public void revokeSession(Long userId, Long sessionId) {
        AuthSession session = authSessionRepository.findByIdAndUser_Id(sessionId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
        session.revoke();
        auditService.recordSystem("USER_SESSION_REVOKED", "AUTH_SESSION", session.getId(), "userId=" + userId);
        emailNotificationService.sendSecurityAlert(session.getUser(), "Session revoked",
                "A session on your account has been revoked.");
    }

    @Override
    @Transactional
    public void revokeAllSessions(Long userId) {
        authSessionRepository.findAllByUser_IdAndRevokedAtIsNull(userId)
                .forEach(AuthSession::revoke);
        auditService.recordSystem("USER_SESSIONS_REVOKED", "USER", userId, null);
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        emailNotificationService.sendSecurityAlert(user, "All sessions revoked",
                "All sessions on your account have been revoked. You need to log in again.");
    }

    @Override
    @Transactional(readOnly = true)
    public long countActiveSessions(Long userId) {
        return authSessionRepository.countByUser_IdAndRevokedAtIsNull(userId);
    }

    private PageRequest paged(int page, int size) {
        return PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 200)));
    }
}
