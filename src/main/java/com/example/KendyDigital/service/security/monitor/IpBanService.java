package com.example.KendyDigital.service.security.monitor;

import com.example.KendyDigital.model.security.IpBan;
import java.time.Duration;
import java.util.List;

public interface IpBanService {
    boolean isBanned(String ip);

    boolean isAllowlisted(String ip);

    IpBan ban(String ipOrCidr, String reason, com.example.KendyDigital.model.security.IpBanSource source,
            String ruleCode, Long adminUserId, Duration ttl);

    void unban(Long id, Long adminUserId);

    List<IpBan> active(int limit);
}
