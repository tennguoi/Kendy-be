package com.example.KendyDigital.service;

import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.KendyDigital.dto.AuthLoginRequest;
import com.example.KendyDigital.dto.AuthRegisterRequest;
import com.example.KendyDigital.dto.AuthTokenResponse;
import com.example.KendyDigital.dto.AuthUserResponse;
import com.example.KendyDigital.model.UserAccount;
import com.example.KendyDigital.model.UserStatus;
import com.example.KendyDigital.repository.UserAccountRepository;

@Service
public class AuthService {
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthTokenService authTokenService;

    public AuthService(UserAccountRepository userAccountRepository, PasswordEncoder passwordEncoder,
            AuthTokenService authTokenService) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.authTokenService = authTokenService;
    }

    @Transactional
    public AuthUserResponse register(AuthRegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userAccountRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }

        UserAccount user = new UserAccount(
                request.name().trim(),
                email,
                blankToNull(request.phone()),
                passwordEncoder.encode(request.password()));
        return AuthUserResponse.from(userAccountRepository.save(user));
    }

    @Transactional
    public AuthTokenResponse login(AuthLoginRequest request) {
        UserAccount user = userAccountRepository.findByEmailIgnoreCase(normalizeEmail(request.email()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is not active");
        }
        AuthTokenService.IssuedToken issuedToken = authTokenService.issue(user);
        return new AuthTokenResponse(issuedToken.token(), issuedToken.expiresAt(), AuthUserResponse.from(user));
    }

    public void logout(String token) {
        authTokenService.revoke(token);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
