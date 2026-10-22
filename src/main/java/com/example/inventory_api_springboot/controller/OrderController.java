package com.example.inventory_api_springboot.controller;

import com.example.inventory_api_springboot.annotations.RequireRole;
import com.example.inventory_api_springboot.dto.request.OrderCreate.OrderCreateRequestDto;
import com.example.inventory_api_springboot.enums.UserRoleEnum;
import com.example.inventory_api_springboot.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("order")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @PostMapping("create")
    @RequireRole(UserRoleEnum.CUSTOMER)
    public ResponseEntity<String> create(
        @Valid @RequestBody OrderCreateRequestDto orderData,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return orderService.create(
            orderData,
            UUID.fromString(jwt.getSubject())
        );
    }

    @PatchMapping("{id}/finish")
    @RequireRole(UserRoleEnum.WAREHOUSE_STAFF)
    public ResponseEntity<String> ship(
        @PathVariable("id") UUID id,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return orderService.finish(
            id,
            UUID.fromString(jwt.getSubject())
        );
    }

    @PatchMapping("{id}/cancel")
    @RequireRole(UserRoleEnum.CUSTOMER)
    public ResponseEntity<String> cancel(
        @PathVariable("id") UUID id
    ) {
        return orderService.cancel(id);
    }
}
