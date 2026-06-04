package com.example.KendyDigital.security;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.KendyDigital.model.UserAccount;
import com.example.KendyDigital.model.UserRole;
import com.example.KendyDigital.repository.SystemSettingRepository;
import com.example.KendyDigital.repository.UserAccountRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class TwoFactorRequiredFilter extends OncePerRequestFilter {
    private static final String ADMIN_PATH_PREFIX = "/api/admin/";
    private static final String[] EXEMPT_PATHS = {
        "/api/admin/admins/",
    };

    private final UserAccountRepository userAccountRepository;
    private final SystemSettingRepository systemSettingRepository;

    public TwoFactorRequiredFilter(UserAccountRepository userAccountRepository,
                                    SystemSettingRepository systemSettingRepository) {
        this.userAccountRepository = userAccountRepository;
        this.systemSettingRepository = systemSettingRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();

        if (path.startsWith(ADMIN_PATH_PREFIX) && !isExemptPath(path)) {
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()
                    && authentication.getPrincipal() instanceof AuthenticatedUser principal) {
                if (principal.role() == UserRole.ADMIN || principal.role() == UserRole.SUPER_ADMIN) {
                    boolean twoFactorRequired = systemSettingRepository.findById("admin_2fa_required")
                            .map(setting -> "true".equalsIgnoreCase(setting.getValue()))
                            .orElse(false);
                    if (twoFactorRequired) {
                        UserAccount admin = userAccountRepository.findById(principal.userId()).orElse(null);
                        if (admin != null && !admin.isTwoFactorEnabled()) {
                            response.sendError(HttpStatus.FORBIDDEN.value(),
                                    "Two-factor authentication must be enabled to access admin features");
                            return;
                        }
                    }
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isExemptPath(String path) {
        for (String exempt : EXEMPT_PATHS) {
            if (path.startsWith(exempt)) {
                String remainder = path.substring(exempt.length());
                return remainder.matches("\\d+/2fa/.*");
            }
        }
        return false;
    }
}
