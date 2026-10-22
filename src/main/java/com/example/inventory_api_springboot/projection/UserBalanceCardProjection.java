package com.example.inventory_api_springboot.projection;

import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;
import java.util.UUID;

public interface UserBalanceCardProjection {
    UUID getUserId();
    BigDecimal getBalance();
    BigDecimal getReservedBalance();

    @Value("#{target.balance - target.reservedBalance}")
    BigDecimal getAvailableBalance();
}
