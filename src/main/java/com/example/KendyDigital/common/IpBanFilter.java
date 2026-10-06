package com.example.KendyDigital.common;

import com.example.KendyDigital.common.error.ApiError;
import com.example.KendyDigital.service.security.monitor.IpBanService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Blocks requests originating from banned IPs/CIDRs. Bans are checked against Redis (O(1)) with a
 * database fallback managed by {@link IpBanService}.
 */
@Component
@Order(1)
public class IpBanFilter extends OncePerRequestFilter {
    private final IpBanService ipBanService;
    private final ClientIpResolver clientIpResolver;
    private final ObjectMapper objectMapper;

    public IpBanFilter(IpBanService ipBanService, ClientIpResolver clientIpResolver, ObjectMapper objectMapper) {
        this.ipBanService = ipBanService;
        this.clientIpResolver = clientIpResolver;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return "OPTIONS".equalsIgnoreCase(request.getMethod())
                || path.startsWith("/actuator/health")
                || path.startsWith("/ws/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String ip = clientIpResolver.resolve(request);
        if (ipBanService.isBanned(ip)) {
            ApiError error = ApiError.of(HttpStatusValue.FORBIDDEN, "Forbidden", "IP_BANNED",
                    "Access from this IP address has been blocked.");
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write(objectMapper.writeValueAsString(error));
            return;
        }
        filterChain.doFilter(request, response);
    }

    private static final class HttpStatusValue {
        static final int FORBIDDEN = 403;
    }
}
