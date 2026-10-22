package com.example.inventory_api_springboot.dto.query.response;

import java.util.List;
import java.util.UUID;

public record OrderReadyProcessDto (
    UUID getId,
    UUID getUserId,
    List<OrderReadyProcessDetailDto> getListDetail
) {}
