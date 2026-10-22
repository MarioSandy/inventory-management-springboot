package com.example.inventory_api_springboot.projection;

import org.springframework.beans.factory.annotation.Value;

import java.util.UUID;

public interface ProductCategoryDropdownProjection {
    UUID getId();

    @Value("#{target.category}")
    String getName();
}
