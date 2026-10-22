package com.example.inventory_api_springboot.dto.queue;

import com.example.inventory_api_springboot.dto.request.ProductCreateRequestDto;

import java.util.List;

public record OutboxProductCreateBatchPayloadDto(
    List<ProductCreateRequestDto> getProducts
) {
}
