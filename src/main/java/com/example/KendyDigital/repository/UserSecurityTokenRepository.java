package com.example.KendyDigital.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.KendyDigital.model.UserSecurityToken;
import com.example.KendyDigital.model.UserSecurityTokenType;

public interface UserSecurityTokenRepository extends JpaRepository<UserSecurityToken, Long> {
    Optional<UserSecurityToken> findByTokenHashAndTypeAndUsedAtIsNull(String tokenHash, UserSecurityTokenType type);

    Optional<UserSecurityToken> findByUser_IdAndTokenHashAndTypeAndUsedAtIsNull(Long userId, String tokenHash,
            UserSecurityTokenType type);
}
