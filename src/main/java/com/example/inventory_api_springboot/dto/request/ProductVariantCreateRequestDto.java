package com.example.inventory_api_springboot.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductVariantCreateRequestDto(
    @NotBlank(message = "Color cannot be empty")
    @Size(max = 20, message = "Color cannot exceeds 20 characters")
    String color,

    @NotBlank(message = "Size cannot be empty")
    @Size(max = 4, message = "Size cannot exceeds 4 characters")
    String size,

    @NotNull(message = "Base price cannot be empty")
    @Positive(message = "Price should be positive")
    BigDecimal price
) {}
