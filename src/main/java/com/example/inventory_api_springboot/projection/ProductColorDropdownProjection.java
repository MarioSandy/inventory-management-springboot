package com.example.inventory_api_springboot.projection;

import org.springframework.beans.factory.annotation.Value;

public interface ProductColorDropdownProjection {
    String getId();

    @Value("#{target.color}")
    String getName();

    String getHexCode();
}
