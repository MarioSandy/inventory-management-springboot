package com.example.inventory_api_springboot.dto.query.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderReadyProcessDetailDto(
    @JsonProperty("id")
    UUID orderDetailId,
    @JsonProperty("product_variant_id")
    UUID productVariantId,

    int quantity,

    @JsonProperty("sub_total")
    BigDecimal subTotal
) {}
