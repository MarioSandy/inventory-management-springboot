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

//    @Query(value =
//        "SELECT p.id, " +
//        "       p.name, " +
//        "       p.description, " +
//        "       p.base_price AS basePrice, " +
//        "       p.\"version\", " +
//        "       pb.name AS brandName, " +
//        "       pc.category AS categoryName, " +
//        "       JSONB_AGG ( " +
//        "           JSONB_BUILD_OBJECT ( " +
//        "               'id', pv.id, " +
//        "               'sku', pv.sku, " +
//        "               'price', pv.price, " +
//        "               'version', pv.version, " +
//        "               'colorName', pcr.color, " +
//        "               'colorHexCode', '#' || pcr.hex_code, " +
//        "               'sizeName', ps.size || ' - ' || ps.description " +
//        "           ) " +
//        "           ORDER BY pcr.color, " +
//        "                    ps.description " +
//        "       ) AS listProductVariants " +
//        "FROM product p " +
//        "JOIN product_variant pv " +
//        "   ON p.id = pv.product_id " +
//        "JOIN product_brand pb " +
//        "   ON p.product_brand_id = pb.id " +
//        "   AND " +
//        "JOIN product_category pc " +
//        "   ON p.product_category_id = pc.id " +
//        "JOIN product_color pcr " +
//        "   ON pv.color = pcr.id " +
//        "JOIN product_size ps " +
//        "   ON pv.\"size\" = ps.\"size\" " +
//        "GROUP BY p.id, " +
//        "         p.name, " +
//        "         p.description, " +
//        "         p.base_price, " +
//        "         p.\"version\", " +
//        "         pb.name, " +
//        "         pc.category " ,
//        nativeQuery = true
//    )
//    public List<ProductTableProjection> findTableData();

//    @Query(
//       value =
//           "SELECT pv.id, " +
//           "       COALESCE(pv.price, p.basePrice) AS price " +
//           "FROM Product p " +
//           "JOIN p.listProductVariants pv " +
//           "    ON p.id = pv.productId " +
//           "    AND pv.id IN :listProductVariantIds " +
//           "    AND pv.deletedAt IS NULL "
//    )
//    List<ProductVariantPriceProjection> findListIdAndPrice(
//        @Param("listProductVariantIds") Set<UUID> listProductVariantIds
//    );

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
