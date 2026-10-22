package com.example.inventory_api_springboot.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProductSizeCreateRequestDto (
    @NotBlank(message = "Product size id cannot be empty")
    @Size(max = 4, message = "Product size cannot be more than 4 characters long")
    String size,

    @NotBlank(message = "Product size description cannot be empty")
    String description
) { }
