package com.example.KendyDigital.security;

import com.example.KendyDigital.service.security.UserSecurityService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
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
            userSecurityService.resolveApiKey(apiKey).ifPresent(user -> {
                AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getEmail(), user.getRole());
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        principal,
                        apiKey,
                        List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            });
        }
        filterChain.doFilter(request, response);
    }
}
