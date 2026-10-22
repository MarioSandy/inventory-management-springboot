package com.example.inventory_api_springboot.dto.request.OrderCreate;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderCreateDetailRequestDto(
    @JsonProperty("product_variant_id")
    @NotNull(message = "Product variant id cannot be empty")
    UUID productVariantId,

    @NotNull(message = "Quantity cannot be empty")
    @Positive(message = "Quantity should be positive")
    int quantity,

    @NotNull(message = "Price cannot be empty")
    @Positive(message = "Price should be positive")
    BigDecimal price
) {
}
