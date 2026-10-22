package com.example.inventory_api_springboot.controller;

import com.example.inventory_api_springboot.annotations.RequireRole;
import com.example.inventory_api_springboot.dto.request.LoginRequestDTO;
import com.example.inventory_api_springboot.dto.request.RegisterAdminRequestDTO;
import com.example.inventory_api_springboot.dto.request.RegisterCustomerRequestDTO;
import com.example.inventory_api_springboot.enums.UserRoleEnum;
import com.example.inventory_api_springboot.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("register")
    public ResponseEntity<String> register(
        @Valid @RequestBody RegisterCustomerRequestDTO registerRequestDTO
    ) {
        return authService.register(registerRequestDTO);
    }

    @RequireRole(UserRoleEnum.USER_ADMIN)
    @PostMapping("register-admin")
    public ResponseEntity<String> registerAdmin(
        @Valid @RequestBody RegisterAdminRequestDTO registerAdminRequestDTO
    ){
        return authService.registerAdmin(registerAdminRequestDTO);
    }

    @PostMapping("login")
    public ResponseEntity login(
        HttpServletRequest request,
        @Valid @RequestBody LoginRequestDTO loginRequestDTO
    ) {
        return authService.login(request, loginRequestDTO);
    }

    @PostMapping("refresh")
    public ResponseEntity refresh(
        @AuthenticationPrincipal Jwt jwt
    ) throws BadRequestException {
        return authService.refresh(
            UUID.fromString(jwt.getSubject()),
            UUID.fromString(jwt.getClaims().get("session_id").toString())
        );
    }

    @PatchMapping("logout")
    public ResponseEntity<String> logout(
        @AuthenticationPrincipal Jwt jwt
    ) throws BadRequestException {
        return authService.logout(
            UUID.fromString(jwt.getSubject()),
            UUID.fromString(jwt.getClaims().get("session_id").toString())
        );
    }

    @PatchMapping("logout/all")
    public ResponseEntity<String> logoutAll(
        @AuthenticationPrincipal Jwt jwt
    ) throws BadRequestException {
        return authService.logoutAll(UUID.fromString(jwt.getSubject()));
    }
}
