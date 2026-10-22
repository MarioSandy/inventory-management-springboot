package com.example.inventory_api_springboot.aspects;

import com.example.inventory_api_springboot.annotations.RequireRole;
import com.example.inventory_api_springboot.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Aspect
@Component
@RequiredArgsConstructor
public class UserRoleCheckAspect {

    private final AuthService authService; // Your DB repository

    // Intercepts methods annotated with @RequireRoles and reads the annotation values
    @Before("@annotation(requireRoles)")
    public void checkMethodUserRoles(RequireRole requireRoles) {
        validateRole(requireRoles);
    }

    // Intercepts class annotated with @RequireRoles and reads the annotation values
    @Before("@within(requireRoles) && !@annotation(requireRoles)")
    public void checkClassUserRoles(RequireRole requireRoles) {
        validateRole(requireRoles);
    }

    private void validateRole(RequireRole requiredRole) {
        System.out.println("tes");
        // 1. Get Authentication from Spring Security Context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication instanceof JwtAuthenticationToken)) {
            throw new SecurityException("Unauthorized: No authentication context found.");
        }

        System.out.println(authentication.getPrincipal() instanceof Jwt jwt);
        // 2. Extract User ID from the JWT payload principal
        UUID userId;
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            // Adjust "sub" or "userId" based on your exact JWT payload key
            userId = UUID.fromString(jwt.getClaimAsString("sub"));
        } else {
            throw new SecurityException("Unauthorized: Invalid token format.");
        }

        // 4. Verify if the user possesses at least one required role
        boolean hasPermission = authService.roleCheck(userId, requiredRole.value());

        if (!hasPermission) {
            throw new SecurityException("Forbidden: You do not have the required role : " + requiredRole);
        }
    }
}
