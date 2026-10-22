package com.example.inventory_api_springboot.dto.query.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ProductTableDto(
   UUID id,
   String name,
   String description,
   BigDecimal basePrice,
   Long version,
   String brandName,
   String categoryName,
   List<ProductTableVariantDto> listProductDetails
) {}
