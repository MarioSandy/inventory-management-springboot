package com.example.inventory_api_springboot.dto.query.response;

import java.math.BigDecimal;

public record ProductTableVariantDto(
    String id,
    String sku,
    BigDecimal price,
    Long version,
    String colorName,
    String colorHexCode,
    String sizeName
) { }
