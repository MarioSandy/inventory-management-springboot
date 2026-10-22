package com.example.inventory_api_springboot.dto.request;

import jakarta.validation.constraints.Negative;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UserBalanceWithdrawRequestDto (
    @NotNull(message = "Balance cannot be empty")
    @Negative(message = "Balance should be negative")
    BigDecimal balance
) { }
