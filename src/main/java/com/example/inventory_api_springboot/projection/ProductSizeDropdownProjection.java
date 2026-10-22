package com.example.inventory_api_springboot.projection;

import org.springframework.beans.factory.annotation.Value;

public interface ProductSizeDropdownProjection {
    @Value("#{target.size}")
    String getId();

    @Value("#{target.description}")
    String getName();
}
