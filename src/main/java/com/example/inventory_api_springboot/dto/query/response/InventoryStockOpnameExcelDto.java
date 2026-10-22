package com.example.inventory_api_springboot.dto.query.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record InventoryStockOpnameExcelDto(
    UUID getId,
    String getName,
    String getSku,
    int getQuantity,
    int getReservedQuantity,
    int getMinimumActualQuantity
) {}
