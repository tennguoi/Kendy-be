package com.example.KendyDigital.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class ServerTimeFilter extends OncePerRequestFilter {
    public static final String SERVER_TIME_HEADER = "X-Server-Time";

    private final Clock clock;

    public ServerTimeFilter(Clock clock) {
        this.clock = clock;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        response.setHeader(SERVER_TIME_HEADER, Long.toString(clock.millis()));
        response.setHeader("Cache-Control", "no-store");
        filterChain.doFilter(request, response);
    }
}
