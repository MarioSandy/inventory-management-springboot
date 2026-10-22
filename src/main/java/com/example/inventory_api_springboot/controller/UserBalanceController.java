package com.example.inventory_api_springboot.controller;

import com.example.inventory_api_springboot.annotations.RequireRole;
import com.example.inventory_api_springboot.dto.request.UserBalanceDepositRequestDto;
import com.example.inventory_api_springboot.dto.request.UserBalanceWithdrawRequestDto;
import com.example.inventory_api_springboot.enums.UserRoleEnum;
import com.example.inventory_api_springboot.projection.UserBalanceCardProjection;
import com.example.inventory_api_springboot.service.UserBalanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("user/balance")
@RequiredArgsConstructor
public class UserBalanceController {
    private final UserBalanceService userBalanceService;

    @GetMapping()
    @RequireRole(UserRoleEnum.CUSTOMER)
    public ResponseEntity<UserBalanceCardProjection> getBalance(
            @AuthenticationPrincipal Jwt jwt
    ) {
        return userBalanceService.getBalanceCard(
                UUID.fromString(jwt.getSubject())
        );
    }

    @PostMapping("deposit")
    @RequireRole(UserRoleEnum.CUSTOMER)
    public ResponseEntity<String> deposit(
        @Valid @RequestBody UserBalanceDepositRequestDto depositData,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return userBalanceService.deposit(
            depositData,
            UUID.fromString(jwt.getSubject())
        );
    }

    @PostMapping("withdraw")
    @RequireRole(UserRoleEnum.CUSTOMER)
    public ResponseEntity<String> withdraw(
        @Valid @RequestBody UserBalanceWithdrawRequestDto withdrawData,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return userBalanceService.withdraw(
            withdrawData,
            UUID.fromString(jwt.getSubject())
        );
    }
}
