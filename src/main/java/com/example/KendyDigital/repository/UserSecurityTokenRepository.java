package com.example.KendyDigital.repository;

import com.example.KendyDigital.model.user.UserSecurityToken;
import com.example.KendyDigital.model.user.UserSecurityTokenType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSecurityTokenRepository extends JpaRepository<UserSecurityToken, Long> {
    Optional<UserSecurityToken> findByTokenHashAndTypeAndUsedAtIsNull(String tokenHash, UserSecurityTokenType type);

    Optional<UserSecurityToken> findByUser_IdAndTokenHashAndTypeAndUsedAtIsNull(Long userId, String tokenHash,
            UserSecurityTokenType type);
}
