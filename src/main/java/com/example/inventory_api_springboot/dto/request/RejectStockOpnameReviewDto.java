package com.example.inventory_api_springboot.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RejectStockOpnameReviewDto(
    @JsonProperty("root_cause")
    String rootCause,

    @JsonProperty("investigation_note")
    String investigationNote
) {}
