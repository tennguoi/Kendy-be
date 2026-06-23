package com.example.KendyDigital.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetAddress;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Profile({"prod", "production"})
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ProductionHttpsRedirectFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        boolean trustedProxy = isTrustedProxy(request.getRemoteAddr());
        if (request.isSecure() || (trustedProxy && "https".equalsIgnoreCase(forwardedProto))) {
            filterChain.doFilter(request, response);
            return;
        }

        String query = request.getQueryString();
        String location = "https://" + request.getServerName() + request.getRequestURI()
                + (query == null || query.isBlank() ? "" : "?" + query);
        response.setStatus(HttpServletResponse.SC_PERMANENT_REDIRECT);
        response.setHeader("Location", location);
    }

    private boolean isTrustedProxy(String remoteAddress) {
        try {
            InetAddress address = InetAddress.getByName(remoteAddress);
            return address.isLoopbackAddress() || address.isSiteLocalAddress();
        } catch (Exception ignored) {
            return false;
        }
    }
}
