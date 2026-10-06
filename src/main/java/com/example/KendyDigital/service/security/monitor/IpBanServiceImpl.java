package com.example.KendyDigital.service.security.monitor;

import com.example.KendyDigital.common.ClientIpResolver;
import com.example.KendyDigital.config.AppSecurityProperties;
import com.example.KendyDigital.config.SecurityMonitorProperties;
import com.example.KendyDigital.model.security.IpBan;
import com.example.KendyDigital.model.security.IpBanSource;
import com.example.KendyDigital.repository.IpBanRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IpBanServiceImpl implements IpBanService {
    private static final Logger LOGGER = LoggerFactory.getLogger(IpBanServiceImpl.class);
    private static final String BAN_KEY_PREFIX = "sec:ban:";
    private static final String BAN_FREQ_PREFIX = "sec:banfreq:";

    private final IpBanRepository ipBanRepository;
    private final SecurityCounters counters;
    private final SecurityMonitorProperties monitorProperties;
    private final AppSecurityProperties appSecurityProperties;
    private final ClientIpResolver clientIpResolver;
    private final SecurityEventWriter eventWriter;

    private final CopyOnWriteArrayList<IpBan> cidrCache = new CopyOnWriteArrayList<>();

    public IpBanServiceImpl(IpBanRepository ipBanRepository, SecurityCounters counters,
            SecurityMonitorProperties monitorProperties, AppSecurityProperties appSecurityProperties,
            ClientIpResolver clientIpResolver, SecurityEventWriter eventWriter) {
        this.ipBanRepository = ipBanRepository;
        this.counters = counters;
        this.monitorProperties = monitorProperties;
        this.appSecurityProperties = appSecurityProperties;
        this.clientIpResolver = clientIpResolver;
        this.eventWriter = eventWriter;
    }

    @Override
    public boolean isBanned(String ip) {
        if (ip == null || ip.isBlank() || isAllowlisted(ip)) {
            return false;
        }
        if ("1".equals(counters.get(BAN_KEY_PREFIX + ip))) {
            return true;
        }
        try {
            List<IpBan> exact = ipBanRepository.findActiveByIpOrCidr(ip, Instant.now());
            if (!exact.isEmpty()) {
                counters.set(BAN_KEY_PREFIX + ip, "1", ttlFor(exact.get(0)));
                exact.forEach(IpBan::registerHit);
                ipBanRepository.saveAll(exact);
                return true;
            }
        } catch (RuntimeException exception) {
            LOGGER.debug("Exact IP ban lookup failed for {}", ip, exception);
        }
        for (IpBan ban : cidrCache) {
            if (ban.isActive() && clientIpResolver.matches(ban.getIpOrCidr(), ip)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isAllowlisted(String ip) {
        if (ip == null || ip.isBlank()) {
            return false;
        }
        List<String> allowlist = new ArrayList<>(monitorProperties.getAutoBanAllowlist());
        allowlist.addAll(appSecurityProperties.getIpAllowlist());
        for (String pattern : allowlist) {
            if (clientIpResolver.matches(pattern, ip)) {
                return true;
            }
        }
        return false;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public IpBan ban(String ipOrCidr, String reason, IpBanSource source, String ruleCode, Long adminUserId,
            Duration ttl) {
        if (ipOrCidr == null || ipOrCidr.isBlank()) {
            return null;
        }
        if (source == IpBanSource.AUTO_RULE && (!monitorProperties.isBlockEnabled() || monitorProperties.isDryRun())) {
            LOGGER.info("DRY-RUN: would ban {} rule={} reason={}", ipOrCidr, ruleCode, reason);
            return null;
        }
        if (source == IpBanSource.AUTO_RULE && isAllowlisted(ipOrCidr)) {
            return null;
        }
        Duration effectiveTtl = source == IpBanSource.AUTO_RULE
                ? escalate(ipOrCidr, capTtl(ttl))
                : ttl;
        Instant expiresAt = effectiveTtl == null ? null : Instant.now().plus(effectiveTtl);
        IpBan ban = new IpBan(ipOrCidr, reason, source, ruleCode, adminUserId, expiresAt);
        IpBan saved = ipBanRepository.save(ban);
        if (ipOrCidr.contains("/")) {
            cidrCache.add(saved);
        } else {
            counters.set(BAN_KEY_PREFIX + ipOrCidr, "1",
                    effectiveTtl == null ? Duration.ofDays(3650) : effectiveTtl);
        }
        eventWriter.write(new com.example.KendyDigital.model.security.SecurityEvent(
                com.example.KendyDigital.model.security.SecurityEventType.IP_BANNED,
                com.example.KendyDigital.model.security.SecuritySeverity.HIGH,
                ipOrCidr, null, null, null, null, null, null,
                "rule=" + ruleCode + ",reason=" + reason + ",source=" + source));
        return saved;
    }

    @Override
    @Transactional
    public void unban(Long id, Long adminUserId) {
        ipBanRepository.findById(id).ifPresent(ban -> {
            ban.revoke(adminUserId);
            counters.delete(BAN_KEY_PREFIX + ban.getIpOrCidr());
            cidrCache.removeIf(cached -> cached.getId() != null && cached.getId().equals(id));
            eventWriter.write(new com.example.KendyDigital.model.security.SecurityEvent(
                    com.example.KendyDigital.model.security.SecurityEventType.IP_UNBANNED,
                    com.example.KendyDigital.model.security.SecuritySeverity.INFO,
                    ban.getIpOrCidr(), null, null, null, null, null, null,
                    "adminUserId=" + adminUserId));
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<IpBan> active(int limit) {
        return ipBanRepository.findActive(Instant.now(),
                PageRequest.of(0, Math.max(1, Math.min(limit, 200))));
    }

    @Scheduled(fixedDelay = 30_000)
    public void refreshCidrCache() {
        try {
            List<IpBan> active = ipBanRepository.findActive(Instant.now(), PageRequest.of(0, 500));
            cidrCache.clear();
            active.stream().filter(ban -> ban.getIpOrCidr() != null && ban.getIpOrCidr().contains("/"))
                    .forEach(cidrCache::add);
        } catch (RuntimeException exception) {
            LOGGER.debug("Could not refresh CIDR ban cache", exception);
        }
    }

    private Duration escalate(String ip, Duration base) {
        long count = counters.increment(BAN_FREQ_PREFIX + ip, Duration.ofHours(24));
        if (count <= 1) {
            return base;
        }
        if (count == 2) {
            return Duration.ofHours(6);
        }
        return Duration.ofHours(monitorProperties.getMaxBanHours());
    }

    private Duration capTtl(Duration ttl) {
        Duration max = Duration.ofHours(Math.max(1, monitorProperties.getMaxBanHours()));
        if (ttl == null || ttl.compareTo(max) > 0) {
            return max;
        }
        return ttl;
    }

    private Duration ttlFor(IpBan ban) {
        if (ban.getExpiresAt() == null) {
            return Duration.ofDays(3650);
        }
        Duration remaining = Duration.between(Instant.now(), ban.getExpiresAt());
        return remaining.isNegative() || remaining.isZero() ? Duration.ofSeconds(30) : remaining;
    }
}
