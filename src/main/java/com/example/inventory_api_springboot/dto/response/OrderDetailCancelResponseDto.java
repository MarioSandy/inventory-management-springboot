package com.example.inventory_api_springboot.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderDetailCancelResponseDto (
    UUID orderDetailId,
    UUID orderId,
    UUID userId,
    int cancelledQuantity,
    BigDecimal cancelledSubTotal
) {}
