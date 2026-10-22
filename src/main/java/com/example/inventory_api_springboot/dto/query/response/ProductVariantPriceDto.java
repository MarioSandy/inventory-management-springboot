package com.example.inventory_api_springboot.dto.query.response;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductVariantPriceDto(
   UUID id,
   BigDecimal price
) {}
