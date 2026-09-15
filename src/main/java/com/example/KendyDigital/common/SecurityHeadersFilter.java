package com.example.KendyDigital.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class SecurityHeadersFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // X-Content-Type-Options: Prevent MIME type sniffing
        response.setHeader("X-Content-Type-Options", "nosniff");
        
        // X-Frame-Options: Prevent clickjacking
        response.setHeader("X-Frame-Options", "DENY");
        
        // Referrer-Policy: Control referrer information
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        
        // Permissions-Policy: Control browser features
        response.setHeader("Permissions-Policy", "geolocation=(), microphone=(), camera=()");
        
        // X-XSS-Protection: Legacy XSS protection (for older browsers)
        response.setHeader("X-XSS-Protection", "1; mode=block");
        
        // Cross-Origin-Opener-Policy: Isolate browsing context
        response.setHeader("Cross-Origin-Opener-Policy", "same-origin");
        
        // Cross-Origin-Resource-Policy: Control cross-origin resource loading
        response.setHeader("Cross-Origin-Resource-Policy", "same-origin");
        
        filterChain.doFilter(request, response);
    }
}