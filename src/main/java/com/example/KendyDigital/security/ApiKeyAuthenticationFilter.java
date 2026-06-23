package com.example.KendyDigital.security;

import com.example.KendyDigital.service.security.UserSecurityService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {
    private static final String API_KEY_HEADER = "X-Api-Key";

    private final UserSecurityService userSecurityService;

    public ApiKeyAuthenticationFilter(UserSecurityService userSecurityService) {
        this.userSecurityService = userSecurityService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String apiKey = request.getHeader(API_KEY_HEADER);
        if (apiKey != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            var resolved = userSecurityService.resolveApiKey(apiKey);
            if (resolved.isPresent()) {
                ResolvedApiKey key = resolved.get();
                String requiredScope = requiredScope(request);
                if (request.getRequestURI().startsWith("/api/admin/") || !key.allows(requiredScope)) {
                    response.setStatus(HttpStatus.FORBIDDEN.value());
                    response.setContentType("application/json");
                    response.getWriter().write("{\"message\":\"API key scope does not allow this operation\"}");
                    return;
                }
                var user = key.user();
                AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getEmail(), user.getRole());
                List<SimpleGrantedAuthority> authorities = new java.util.ArrayList<>();
                authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
                key.scopes().forEach(scope ->
                        authorities.add(new SimpleGrantedAuthority("SCOPE_" + scope.replace(':', '_'))));
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        principal,
                        apiKey,
                        authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        filterChain.doFilter(request, response);
    }

    private String requiredScope(HttpServletRequest request) {
        String path = request.getRequestURI();
        boolean read = "GET".equals(request.getMethod()) || "HEAD".equals(request.getMethod());
        if (path.startsWith("/api/orders")) return read ? "orders:read" : "orders:write";
        if (path.startsWith("/api/wallet")) return read ? "wallet:read" : "wallet:write";
        if (path.startsWith("/api/deposits")) return read ? "deposits:read" : "deposits:write";
        if (path.startsWith("/api/tickets")) return read ? "tickets:read" : "tickets:write";
        if (path.startsWith("/api/warranty-requests") || path.contains("/warranty")) {
            return read ? "warranty:read" : "warranty:write";
        }
        if (path.startsWith("/api/notifications")) {
            return read ? "notifications:read" : "notifications:write";
        }
        if (path.startsWith("/api/me/credentials")) return "credentials:read";
        if (path.startsWith("/api/me/entitlements")) {
            return read ? "entitlements:read" : "entitlements:write";
        }
        if (path.startsWith("/api/me/api-keys") || path.startsWith("/api/me/security")
                || path.startsWith("/api/me/sessions") || path.startsWith("/api/me/2fa")) {
            return read ? "security:read" : "security:write";
        }
        if (path.startsWith("/api/me")) return read ? "profile:read" : "profile:write";
        return "api:access";
    }
}
