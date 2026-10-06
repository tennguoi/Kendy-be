package com.example.KendyDigital.common;

import com.example.KendyDigital.model.security.SecurityEventType;
import com.example.KendyDigital.model.security.SecuritySeverity;
import com.example.KendyDigital.security.AuthenticatedUser;
import com.example.KendyDigital.service.security.monitor.SecuritySignal;
import com.example.KendyDigital.service.security.monitor.SecuritySignalService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Observes response status codes to detect object-ID enumeration (repeated 403/404 on numeric
 * resource paths). Detection only.
 */
@Component
@Order(3)
public class SecurityResponseSignalFilter extends OncePerRequestFilter {
    private static final Pattern NUMERIC_RESOURCE = Pattern.compile("^/api/(?!auth/|admin/security/)[^/]+/\\d+(/.*)?$");

    private final SecuritySignalService signalService;
    private final ClientIpResolver clientIpResolver;

    public SecurityResponseSignalFilter(SecuritySignalService signalService, ClientIpResolver clientIpResolver) {
        this.signalService = signalService;
        this.clientIpResolver = clientIpResolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        filterChain.doFilter(request, response);
        int status = response.getStatus();
        if (status != 403 && status != 404) {
            return;
        }
        String path = request.getRequestURI();
        if (!NUMERIC_RESOURCE.matcher(path).matches()) {
            return;
        }
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Long userId = authentication != null
                    && authentication.getPrincipal() instanceof AuthenticatedUser user ? user.userId() : null;
            signalService.record(SecuritySignal
                    .of(SecurityEventType.ID_ENUMERATION, SecuritySeverity.LOW, clientIpResolver.resolve(request))
                    .user(userId)
                    .request(request.getMethod(), path)
                    .userAgent(request.getHeader("User-Agent"))
                    .metadata("status=" + status)
                    .risk(10)
                    .build());
        } catch (RuntimeException ignored) {
        }
    }
}
