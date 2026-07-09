package com.example.KendyDigital.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Profile({"prod", "production"})
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ProductionHttpsRedirectFilter extends OncePerRequestFilter {

    private final String canonicalHost;
    private static final Set<String> ALLOWED_HOSTS = Set.of(
            "kendydigital.com",
            "www.kendydigital.com",
            "api.kendydigital.com",
            "admin.kendydigital.com",
            "localhost");

    public ProductionHttpsRedirectFilter(
            @Value("${app.canonical-host:kendydigital.com}") String canonicalHost) {
        this.canonicalHost = canonicalHost;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        boolean trustedProxy = isTrustedProxy(request.getRemoteAddr());
        if (request.isSecure() || (trustedProxy && "https".equalsIgnoreCase(forwardedProto))) {
            filterChain.doFilter(request, response);
            return;
        }

        String host = request.getServerName();
        if (!ALLOWED_HOSTS.contains(host)) {
            host = canonicalHost;
        }
        String query = request.getQueryString();
        String location = "https://" + host + request.getRequestURI()
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
