package com.example.KendyDigital.common;

import com.example.KendyDigital.model.security.SecurityEventType;
import com.example.KendyDigital.model.security.SecuritySeverity;
import com.example.KendyDigital.service.security.monitor.SecuritySignal;
import com.example.KendyDigital.service.security.monitor.SecuritySignalService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Lightweight web-application-firewall style detector. It never blocks by itself: matches are
 * emitted as signals and the detection rules decide the response (risk, ban, alert).
 */
@Component
@Order(2)
public class AttackPatternFilter extends OncePerRequestFilter {
    private static final int MAX_INSPECT_LENGTH = 2048;

    private record AttackPattern(Pattern pattern, SecurityEventType type, SecuritySeverity severity,
            double riskPoints, String label) {
    }

    private static final List<AttackPattern> PATTERNS = List.of(
            new AttackPattern(Pattern.compile("(?i)(union(\\s|%20|\\+)+select|or(\\s|%20|\\+)+1(\\s|%20|\\+)*=(\\s|%20|\\+)*1|'\\s*or\\s*'|;--|sleep\\(|benchmark\\(|information_schema)"),
                    SecurityEventType.WAF_SQLI, SecuritySeverity.MEDIUM, 30, "SQL_INJECTION"),
            new AttackPattern(Pattern.compile("(?i)(<script|javascript:|onerror\\s*=|onload\\s*=|%3cscript|<svg/onload)"),
                    SecurityEventType.WAF_XSS, SecuritySeverity.MEDIUM, 30, "XSS"),
            new AttackPattern(Pattern.compile("(?i)(\\.\\./|\\.\\.\\\\|%2e%2e%2f|%2e%2e/|/etc/passwd|/proc/self|windows/win\\.ini)"),
                    SecurityEventType.WAF_TRAVERSAL, SecuritySeverity.HIGH, 50, "PATH_TRAVERSAL"),
            new AttackPattern(Pattern.compile("(?i)(/\\.env|/\\.git|/wp-admin|/wp-login|/phpmyadmin|/xmlrpc\\.php|/\\.aws/credentials|/config\\.php)"),
                    SecurityEventType.WAF_SCANNER, SecuritySeverity.HIGH, 60, "SCANNER_PATH"),
            new AttackPattern(Pattern.compile("(?i)(sqlmap|nikto|nuclei|masscan|acunetix|nessus|dirbuster|gobuster|ffuf|wpscan|zgrab)"),
                    SecurityEventType.WAF_TOOL_USER_AGENT, SecuritySeverity.HIGH, 40, "ATTACK_TOOL")
    );

    private static final String HONEYTOKEN_PREFIX = "/api/honeytoken";

    private final SecuritySignalService signalService;
    private final ClientIpResolver clientIpResolver;

    public AttackPatternFilter(SecuritySignalService signalService, ClientIpResolver clientIpResolver) {
        this.signalService = signalService;
        this.clientIpResolver = clientIpResolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            inspect(request);
        } catch (RuntimeException ignored) {
        }
        filterChain.doFilter(request, response);
    }

    private void inspect(HttpServletRequest request) {
        String ip = clientIpResolver.resolve(request);
        String path = request.getRequestURI();
        String query = request.getQueryString();
        String target = (path + (query == null ? "" : "?" + query));
        if (target.length() > MAX_INSPECT_LENGTH) {
            target = target.substring(0, MAX_INSPECT_LENGTH);
        }
        String method = request.getMethod();
        String userAgent = request.getHeader("User-Agent");

        if (path.startsWith(HONEYTOKEN_PREFIX)) {
            signalService.record(SecuritySignal.of(SecurityEventType.HONEYTOKEN_HIT, SecuritySeverity.CRITICAL, ip)
                    .request(method, path)
                    .userAgent(userAgent)
                    .metadata("honeytokenPath=" + path)
                    .risk(100)
                    .build());
            return;
        }

        for (AttackPattern attackPattern : PATTERNS) {
            if (attackPattern.pattern().matcher(target).find()
                    || (attackPattern.type() == SecurityEventType.WAF_TOOL_USER_AGENT
                            && userAgent != null && attackPattern.pattern().matcher(userAgent).find())) {
                signalService.record(SecuritySignal.of(attackPattern.type(), attackPattern.severity(), ip)
                        .request(method, path)
                        .userAgent(userAgent)
                        .metadata("pattern=" + attackPattern.label() + ";target=" + target)
                        .risk(attackPattern.riskPoints())
                        .build());
            }
        }
    }
}
