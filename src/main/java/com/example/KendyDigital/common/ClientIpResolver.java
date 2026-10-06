package com.example.KendyDigital.common;

import com.example.KendyDigital.config.AppSecurityProperties;
import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Single source of truth for resolving the real client IP. Used by rate limiting, audit logging,
 * and the security monitoring pipeline so every subsystem agrees on the client identity.
 *
 * <p>Resolution order:
 * <ol>
 *   <li>{@code CF-Connecting-IP} when the direct peer is a trusted proxy (Cloudflare deployments).</li>
 *   <li>{@code X-Forwarded-For} right-to-left, skipping trusted proxies.</li>
 *   <li>{@code X-Real-IP} when the peer is trusted.</li>
 *   <li>The raw socket address otherwise.</li>
 * </ol>
 */
@Component
public class ClientIpResolver {
    public static final String UNKNOWN = "unknown";

    private final Set<String> trustedProxies;
    private final boolean trustCloudflare;

    public ClientIpResolver(AppSecurityProperties properties) {
        this.trustedProxies = Set.copyOf(properties.getTrustedProxies());
        this.trustCloudflare = properties.isTrustCloudflare();
    }

    public String resolve(HttpServletRequest request) {
        if (request == null) {
            return UNKNOWN;
        }
        String remoteAddr = request.getRemoteAddr();
        if (!isTrustedProxy(remoteAddr)) {
            return normalize(remoteAddr);
        }
        if (trustCloudflare) {
            String cf = request.getHeader("CF-Connecting-IP");
            if (cf != null && !cf.isBlank()) {
                return normalize(cf.trim());
            }
        }
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            String[] parts = forwardedFor.split(",");
            for (int i = parts.length - 1; i >= 0; i--) {
                String candidate = parts[i].trim();
                if (!candidate.isEmpty() && !isTrustedProxy(candidate)) {
                    return normalize(candidate);
                }
            }
            return normalize(parts[parts.length - 1].trim());
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return normalize(realIp.trim());
        }
        return normalize(remoteAddr);
    }

    /** Resolve the IP of the current request from the request context, or {@link #UNKNOWN}. */
    public String resolveCurrent() {
        try {
            RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
            if (attributes instanceof ServletRequestAttributes servletAttributes) {
                return resolve(servletAttributes.getRequest());
            }
        } catch (RuntimeException ignored) {
        }
        return UNKNOWN;
    }

    public String currentRequestId() {
        try {
            RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
            if (attributes instanceof ServletRequestAttributes servletAttributes) {
                return servletAttributes.getRequest().getHeader(RequestIdFilter.REQUEST_ID_HEADER);
            }
        } catch (RuntimeException ignored) {
        }
        return null;
    }

    public boolean isTrustedProxy(String address) {
        if (address == null || address.isBlank()) {
            return false;
        }
        String candidate = address.trim();
        if ("127.0.0.1".equals(candidate) || "0:0:0:0:0:0:0:1".equals(candidate) || "::1".equals(candidate)) {
            return true;
        }
        if (trustedProxies.contains(candidate)) {
            return true;
        }
        if (trustedProxies.stream().anyMatch(cidr -> cidrMatches(cidr, candidate))) {
            return true;
        }
        try {
            InetAddress inetAddress = InetAddress.getByName(candidate);
            if (inetAddress.isLoopbackAddress()) {
                return true;
            }
        } catch (UnknownHostException ignored) {
        }
        return false;
    }

    private boolean cidrMatches(String cidr, String candidate) {
        if (cidr == null || !cidr.contains("/")) {
            return false;
        }
        try {
            String[] segments = cidr.split("/", 2);
            byte[] network = InetAddress.getByName(segments[0]).getAddress();
            int prefix = Integer.parseInt(segments[1]);
            byte[] address = InetAddress.getByName(candidate).getAddress();
            if (network.length != address.length) {
                return false;
            }
            int fullBytes = prefix / 8;
            int remainingBits = prefix % 8;
            for (int i = 0; i < fullBytes; i++) {
                if (network[i] != address[i]) {
                    return false;
                }
            }
            if (remainingBits == 0) {
                return true;
            }
            int mask = 0xFF << (8 - remainingBits) & 0xFF;
            return (network[fullBytes] & mask) == (address[fullBytes] & mask);
        } catch (RuntimeException | UnknownHostException ignored) {
            return false;
        }
    }

    /** True when {@code pattern} is either an exact IP match or a CIDR block containing {@code ip}. */
    public boolean matches(String pattern, String ip) {
        if (pattern == null || ip == null || pattern.isBlank() || ip.isBlank()) {
            return false;
        }
        if (pattern.equals(ip)) {
            return true;
        }
        return cidrMatches(pattern, ip);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN;
        }
        String trimmed = value.trim();
        return trimmed.length() > 45 ? trimmed.substring(0, 45) : trimmed;
    }

    /** Convenience accessor for configuration/tests. */
    public List<String> configuredTrustedProxies() {
        return List.copyOf(trustedProxies);
    }
}
