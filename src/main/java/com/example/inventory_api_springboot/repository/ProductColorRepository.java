package com.example.inventory_api_springboot.repository;

import com.example.inventory_api_springboot.entity.ProductColor;
import com.example.inventory_api_springboot.projection.ProductBrandDropdownProjection;
import com.example.inventory_api_springboot.projection.ProductColorDropdownProjection;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface ProductColorRepository extends JpaRepository<ProductColor, String> {
    public Optional<ProductColor> findById(@Param("id") String id);

    @Transactional
    @Modifying
    @Query(value =
            "UPDATE product_color " +
                    "   SET deleted_at = null, " +
                    "       updated_at = NOW() " +
                    "WHERE id = :id",
            nativeQuery = true
    )
    int restoreById(@Param("id") String id);

    List<ProductColorDropdownProjection> findByColorContainingIgnoreCase(String color, Pageable pageable);
}
