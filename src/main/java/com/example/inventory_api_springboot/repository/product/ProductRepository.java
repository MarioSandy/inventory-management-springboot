package com.example.inventory_api_springboot.repository.product;

import com.example.inventory_api_springboot.dto.query.response.ProductVariantPriceDto;
import com.example.inventory_api_springboot.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID>, ProductNativeRepository {
    public Optional<Product> findById(@Param("id") UUID id);

    @Transactional
    @Modifying
    @Query(value =
        "UPDATE product " +
                "   SET deleted_at = null, " +
                "       updated_at = NOW() " +
                "WHERE id = :id",
        nativeQuery = true
    )
    int restoreById(@Param("id") UUID id);

    @Transactional
    @Modifying
    @Query(value =
            "UPDATE product_variant " +
                    "   SET deleted_at = null, " +
                    "       updated_at = NOW() " +
                    "WHERE id = :id",
            nativeQuery = true
    )
    int restoreVariantById(@Param("id") String id);

    @Query(
        value =
            "SELECT pv.id, " +
            "       COALESCE(pv.price, p.basePrice) AS price " +
            "FROM Product p " +
            "JOIN p.listProductVariants pv " +
            "WHERE pv.id IN :listProductVariantIds " +
            "    AND pv.deletedAt IS NULL "
    )
    List<ProductVariantPriceDto> findListIdAndPrice(
        Set<UUID> listProductVariantIds
    );
}
