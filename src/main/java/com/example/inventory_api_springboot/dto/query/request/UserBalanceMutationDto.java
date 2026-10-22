package com.example.inventory_api_springboot.dto.query.request;

import com.example.inventory_api_springboot.enums.UserBalanceLogTypeEnum;

import java.math.BigDecimal;
import java.util.UUID;

public record UserBalanceMutationDto(
    BigDecimal balance,
    BigDecimal reservedBalance,
    UserBalanceLogTypeEnum type,
    UUID userId,
    UUID logCreatorId,
    UUID referenceId
) {}
