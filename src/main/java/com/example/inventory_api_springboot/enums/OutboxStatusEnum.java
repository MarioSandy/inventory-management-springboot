package com.example.inventory_api_springboot.enums;

public enum OutboxStatusEnum {
    PENDING,
    PROCESSING,
    DISPATCHED,
    DISPATCH_FAILED
}
