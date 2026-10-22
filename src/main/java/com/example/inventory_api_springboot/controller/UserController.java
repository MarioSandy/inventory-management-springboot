package com.example.inventory_api_springboot.controller;

import com.example.inventory_api_springboot.annotations.RequireRole;
import com.example.inventory_api_springboot.enums.UserRoleEnum;
import com.example.inventory_api_springboot.projection.UserDetailProjection;
import com.example.inventory_api_springboot.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("detail")
    @RequireRole(UserRoleEnum.CUSTOMER)
    public ResponseEntity<UserDetailProjection> getUserDetail(
        @AuthenticationPrincipal Jwt jwt
    ) {
        return userService.getUserDetail(
            UUID.fromString(jwt.getSubject())
        );
    }
}
