package com.example.inventory_api_springboot.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProductBrandCreateRequestDto (
    @NotBlank(message = "Brand id cannot be empty")
    @Size(min = 4, max = 4, message = "Brand should be 4 characters long")
    String id,

    @NotBlank(message = "Brand name cannot be empty")
    String name,

    String description
) {}
