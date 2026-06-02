package com.example.KendyDigital.security;



import com.example.KendyDigital.service.*;
import com.example.KendyDigital.model.*;
public record AuthenticatedUser(
        Long userId,
        String email,
        UserRole role) {
}
