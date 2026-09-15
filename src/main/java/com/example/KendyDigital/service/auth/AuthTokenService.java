package com.example.KendyDigital.service.auth;

import com.example.KendyDigital.model.user.UserAccount;
import com.example.KendyDigital.repository.*;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.util.Optional;

public interface AuthTokenService {
    IssuedToken issue(UserAccount user, HttpServletResponse response);
    Optional<UserAccount> resolveUser(String token);
    void revoke(String token, HttpServletResponse response);

    record IssuedToken(String token, Instant expiresAt) {
    }
}
