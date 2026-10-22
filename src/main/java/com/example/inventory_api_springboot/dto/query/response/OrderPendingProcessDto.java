package com.example.inventory_api_springboot.dto.query.response;

import java.util.UUID;

public record OrderPendingProcessDto(
    UUID orderId,
    UUID userId
) { }
