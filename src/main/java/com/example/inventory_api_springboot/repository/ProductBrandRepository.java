package com.example.inventory_api_springboot.repository;

import com.example.inventory_api_springboot.entity.ProductBrand;
import com.example.inventory_api_springboot.projection.ProductBrandDropdownProjection;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductBrandRepository extends JpaRepository<ProductBrand, UUID> {
    public Optional<ProductBrand> findById(@Param("id") String id);

    @Transactional
    @Modifying
    @Query(value =
        "UPDATE product_brand " +
        "   SET deleted_at = null, " +
        "       updated_at = NOW() " +
        "WHERE id = :id",
        nativeQuery = true
    )
    int restoreById(@Param("id") String id);

    List<ProductBrandDropdownProjection> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
