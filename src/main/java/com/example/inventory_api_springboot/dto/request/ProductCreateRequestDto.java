package com.example.inventory_api_springboot.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ProductCreateRequestDto (
    @NotBlank(message = "Name cannot be empty")
    @Size(max = 150, message = "Name cannot exceeds 150 characters")
    String name,

    @Size(max = 255, message = "Description cannot exceeds 250 characters")
    String description,

    @NotNull(message = "Category cannot be empty")
    @JsonProperty("product_category_id")
    UUID productCategoryId,

    @NotNull(message = "Brand cannot be empty")
    @JsonProperty("product_brand_id")
    UUID productBrandId,

    @NotNull(message = "Base price cannot be empty")
    @Positive(message = "Base price should be positive")
    @JsonProperty("base_price")
    BigDecimal basePrice,

    @NotEmpty(message = "Detail cannot be empty")
    @Valid
    @JsonProperty("list_detail")
    List<ProductVariantCreateRequestDto> listDetail
) {}
