package com.example.inventory_api_springboot.repository.product;

import com.example.inventory_api_springboot.dto.query.response.ProductBulkInsertReturningDto;
import com.example.inventory_api_springboot.dto.query.response.ProductInvalidMasterDataIdDto;
import com.example.inventory_api_springboot.dto.query.response.ProductTableDto;
import com.example.inventory_api_springboot.dto.query.response.ProductVariantPriceDto;
import com.example.inventory_api_springboot.entity.ProductVariant;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface ProductNativeRepository {
    List<ProductTableDto> findTableData(
        String search,
        String brandId,
        String categoryId,
        String colorId,
        String size,
        String cursor,
        int limit
    );

    List<ProductInvalidMasterDataIdDto> checkInvalidMasterDataIds(
        Set<String> listSizeId,
        Set<String> listColorId,
        Set<UUID> listCategoryId,
        Set<UUID> listBrandId
    );

    List<ProductBulkInsertReturningDto> insertBulkProducts(
        String[] listProductName,
        String[] listProductDescription,
        UUID[] listProductCategoryId,
        UUID[] listProductBrandId,
        BigDecimal[] listProductBasePrice
    );

    int[][] insertBulkProductVariants(List<ProductVariant> listProductVariant);
}
