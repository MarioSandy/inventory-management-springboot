package com.example.inventory_api_springboot.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ProductCategoryCreateRequestDto (
    @NotBlank(message = "Product category cannot be empty")
    String category,
    String description
) {}
