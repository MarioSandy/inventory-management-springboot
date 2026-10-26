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

    private final AuthService authService;

    @Before("@annotation(requireRoles)")
    public void checkMethodUserRoles(RequireRole requireRoles) {
        validateRole(requireRoles);
    }

    @Before("@within(requireRoles) && !@annotation(requireRoles)")
    public void checkClassUserRoles(RequireRole requireRoles) {
        validateRole(requireRoles);
    }

    private void validateRole(RequireRole requiredRole) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication instanceof JwtAuthenticationToken)) {
            throw new SecurityException("Unauthorized: No authentication context found.");
        }

        UUID userId;
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            userId = UUID.fromString(jwt.getClaimAsString("sub"));
        } else {
            throw new SecurityException("Unauthorized: Invalid token format.");
        }

        boolean hasPermission = authService.roleCheck(userId, requiredRole.value());

        if (!hasPermission) {
            throw new SecurityException("Forbidden: You do not have the required role : " + requiredRole);
        }
    }
}
