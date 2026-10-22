package com.example.inventory_api_springboot.dto.query.request;

import java.util.UUID;

public record InventoryStockOpnameDto(
    UUID productVariantId,
    int systemQuantity,
    int actualQuantity,
    int systemReservedQuantity
) { }
