package com.example.inventory_api_springboot.projection;

import java.util.UUID;

public interface ProductBrandDropdownProjection {
    UUID getId();
    String getName();
    String getCode();
}
