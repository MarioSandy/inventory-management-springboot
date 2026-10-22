package com.example.inventory_api_springboot.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProductColorCreateRequestDto (
    @NotBlank(message = "Product color id cannot be empty")
    @Size(max = 5, message = "Product color id cannot be more than 5 characters")
    String id,

    @NotBlank(message = "Product color cannot be empty")
    @Size(max = 20, message = "Product color cannot be more than 20 characters")
    String color,

    @NotBlank(message = "Product color hex code cannot be empty")
    @Pattern(
        regexp = "^([A-Fa-f0-9]{3}|[A-Fa-f0-9]{6})$",
        message = "Invalid product hex color format. Must be # followed by 3 or 6 hex digits."
    )
    @JsonProperty("hex_code")
    String hexCode
) { }
