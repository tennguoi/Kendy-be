package com.example.KendyDigital.security;



import com.example.KendyDigital.service.*;
import com.example.KendyDigital.model.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

public final class CurrentUser {
    private CurrentUser() {
    }

    public static AuthenticatedUser require(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return user;
    }
}
