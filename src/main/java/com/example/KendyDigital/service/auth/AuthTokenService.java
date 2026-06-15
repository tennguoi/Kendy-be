package com.example.KendyDigital.service.auth;

import com.example.KendyDigital.model.user.UserAccount;
import com.example.KendyDigital.repository.*;
import java.time.Instant;
import java.util.Optional;

public interface AuthTokenService {
    IssuedToken issue(UserAccount user);
    Optional<UserAccount> resolveUser(String token);
    void revoke(String token);

    record IssuedToken(String token, Instant expiresAt) {
    }
}
