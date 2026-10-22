package com.example.inventory_api_springboot.repository;

import com.example.inventory_api_springboot.entity.ProductSize;
import com.example.inventory_api_springboot.projection.ProductSizeDropdownProjection;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface ProductSizeRepository extends JpaRepository<ProductSize, String> {
    public Optional<ProductSize> findById(@Param("size") String id);

    @Transactional
    @Modifying
    @Query(value =
            "UPDATE product_size " +
                    "   SET deleted_at = null, " +
                    "       updated_at = NOW() " +
                    "WHERE size = :size",
            nativeQuery = true
    )
    int restoreById(@Param("size") String id);

    List<ProductSizeDropdownProjection> findBySizeContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String size, String description, Pageable pageable);
}
