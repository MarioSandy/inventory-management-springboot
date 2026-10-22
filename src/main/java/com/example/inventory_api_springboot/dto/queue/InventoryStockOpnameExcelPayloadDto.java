package com.example.inventory_api_springboot.dto.queue;

import java.util.UUID;

public record InventoryStockOpnameExcelPayloadDto (
    UUID productVariantId,
    int actualQuantity,
    int rowNumber
) {}
