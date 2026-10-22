package com.example.inventory_api_springboot.dto.request.OrderCreate;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record OrderCreateRequestDto (
    @JsonProperty("list_detail")
    @NotNull(message = "Detail cannot be empty")
    List<OrderCreateDetailRequestDto> listDetail
) {}
