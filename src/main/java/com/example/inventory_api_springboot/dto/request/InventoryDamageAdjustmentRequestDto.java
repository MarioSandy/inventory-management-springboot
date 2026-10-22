package com.example.inventory_api_springboot.dto.request;

import jakarta.validation.constraints.Negative;
import jakarta.validation.constraints.NotNull;

public record InventoryDamageAdjustmentRequestDto(
    @NotNull(message = "Quantity cannot be empty")
    @Negative(message = "Quantity should be negative")
    int quantity
) { }
