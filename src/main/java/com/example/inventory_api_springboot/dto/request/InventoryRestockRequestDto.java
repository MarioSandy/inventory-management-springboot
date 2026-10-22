package com.example.inventory_api_springboot.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record InventoryRestockRequestDto (
    @NotNull(message = "Quantity cannot be empty")
    @Positive(message = "Quantity should be positive")
    int quantity
) {}
