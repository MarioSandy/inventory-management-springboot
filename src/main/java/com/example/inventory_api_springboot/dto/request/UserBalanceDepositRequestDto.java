package com.example.inventory_api_springboot.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record UserBalanceDepositRequestDto(
    @NotNull(message = "Balance cannot be empty")
    @Positive(message = "Balance should be positive")
    BigDecimal balance
) { }
