package com.example.inventory_api_springboot.projection;

import java.math.BigDecimal;
import java.util.UUID;

public interface ProductTableProjection {
    UUID getId();
    String getName();
    String getDescription();
    BigDecimal getBasePrice();
    Long getVersion();
    String getBrandName();
    String getCategoryName();
    String getListProductVariants();
}
