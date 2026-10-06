package com.example.KendyDigital.dto.monitoring.response;

import com.example.KendyDigital.model.security.IpBan;
import java.time.Instant;

public record SecurityIpBanResponse(
        Long id,
        String ipOrCidr,
        String reason,
        String source,
        String ruleCode,
        Long createdBy,
        Instant createdAt,
        Instant expiresAt,
        long hitCount,
        boolean active) {
    public static SecurityIpBanResponse from(IpBan ban) {
        return new SecurityIpBanResponse(
                ban.getId(),
                ban.getIpOrCidr(),
                ban.getReason(),
                ban.getSource() == null ? null : ban.getSource().name(),
                ban.getRuleCode(),
                ban.getCreatedBy(),
                ban.getCreatedAt(),
                ban.getExpiresAt(),
                ban.getHitCount(),
                ban.isActive());
    }
}
