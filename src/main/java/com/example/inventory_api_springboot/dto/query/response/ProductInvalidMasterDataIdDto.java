package com.example.inventory_api_springboot.dto.query.response;

public record ProductInvalidMasterDataIdDto(
    String getEntityType,
    String getId
) {}
