package com.example.inventory_api_springboot.enums;

public enum OutboxEventTypeEnum {
    CREATE_PRODUCT_BATCH,
    STOCK_OPNAME,
    ORDER_PROCESSING,
    FILE_ORCHESTRA_PRODUCT_BATCH_CREATE,
    FILE_ORCHESTRA_INVENTORY_STOCK_OPNAME
}
