package com.example.inventory_api_springboot.repository;

import com.example.inventory_api_springboot.entity.ProductCategory;
import com.example.inventory_api_springboot.projection.ProductBrandDropdownProjection;
import com.example.inventory_api_springboot.projection.ProductCategoryDropdownProjection;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductCategoryRepository extends JpaRepository<ProductCategory, UUID> {
    public Optional<ProductCategory> findById(@Param("id") UUID id);

    @Transactional
    @Modifying
    @Query(value =
            "UPDATE product_category " +
                    "   SET deleted_at = null, " +
                    "       updated_at = NOW() " +
                    "WHERE id = :id",
            nativeQuery = true
    )
    int restoreById(@Param("id") UUID id);

    List<ProductCategoryDropdownProjection> findByCategoryContainingIgnoreCase(String category, Pageable pageable);
}
