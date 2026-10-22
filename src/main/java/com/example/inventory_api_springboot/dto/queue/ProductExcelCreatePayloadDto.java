package com.example.inventory_api_springboot.dto.queue;

import com.example.inventory_api_springboot.dto.request.ProductCreateRequestDto;

public record ProductExcelCreatePayloadDto(
  ProductCreateRequestDto productCreateData,
  int rowNumber
) {}
