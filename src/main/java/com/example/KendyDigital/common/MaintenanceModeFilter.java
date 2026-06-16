package com.example.KendyDigital.common;

import com.example.KendyDigital.repository.SystemSettingRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class MaintenanceModeFilter extends OncePerRequestFilter {
    private final SystemSettingRepository systemSettingRepository;

    public MaintenanceModeFilter(SystemSettingRepository systemSettingRepository) {
        this.systemSettingRepository = systemSettingRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!request.getRequestURI().startsWith("/api/")) {
            filterChain.doFilter(request, response);
            return;
        }
        if (!isMaintenanceEnabled() || isAllowedDuringMaintenance(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
        response.setContentType("application/json");
        response.getWriter().write("{\"message\":\"Hệ thống đang bảo trì. Vui lòng quay lại sau.\"}");
    }

    private boolean isMaintenanceEnabled() {
        return systemSettingRepository.findById("maintenance.enabled")
                .map(setting -> "true".equalsIgnoreCase(setting.getValue()))
                .orElse(false);
    }

    private boolean isAllowedDuringMaintenance(HttpServletRequest request) {
        String path = request.getRequestURI();
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        return path.startsWith("/api/admin/")
                || path.startsWith("/api/auth/")
                || path.startsWith("/api/webhooks/")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || "/api/admin/health".equals(path);
    }
}
