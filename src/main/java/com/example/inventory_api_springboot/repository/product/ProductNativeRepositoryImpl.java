package com.example.inventory_api_springboot.repository.product;

import com.example.inventory_api_springboot.dto.query.response.ProductBulkInsertReturningDto;
import com.example.inventory_api_springboot.dto.query.response.ProductInvalidMasterDataIdDto;
import com.example.inventory_api_springboot.dto.query.response.ProductTableDto;
import com.example.inventory_api_springboot.dto.query.response.ProductTableVariantDto;
import com.example.inventory_api_springboot.dto.query.response.ProductVariantPriceDto;
import com.example.inventory_api_springboot.entity.ProductVariant;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ProductNativeRepositoryImpl implements ProductNativeRepository {
    @PersistenceContext
    private EntityManager entityManager;

    private final JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper;

    @Override
    public List<ProductTableDto> findTableData(
        String search,
        String brandId,
        String categoryId,
        String colorId,
        String size,
        String cursor,
        int limit
    ) {
        String queryBrand =
            brandId != null
                ? "AND pb.id = :brandId "
                : "";

        String queryCategory =
            categoryId != null
                ? "AND pc.id = :categoryId "
                : "";

        String queryColor =
            colorId != null
                ? "AND pcr.id = :colorId "
                : "";

        String querySize =
            size != null
                ? "AND ps.\"size\" = :sizeId "
                : "";

        String queryCursor =
            cursor != null
                    ? "WHERE :cursor < p.id "
                    : "";

        String querySearch = "";

        if (
            search != null
            && cursor == null
        ) {
            querySearch = "WHERE p.name LIKE :search ";
        } else if (
            search != null
            && cursor != null
        ) {
            querySearch = "AND p.name LIKE :search ";
        }

        String queryString =
            "SELECT p.id, " +
            "       p.name, " +
            "       p.description, " +
            "       p.base_price AS basePrice, " +
            "       p.\"version\", " +
            "       pb.name AS brandName, " +
            "       pc.category AS categoryName, " +
            "       JSONB_AGG ( " +
            "           JSONB_BUILD_OBJECT ( " +
            "               'id', pv.id, " +
            "               'sku', pv.sku, " +
            "               'price', pv.price, " +
            "               'version', pv.version, " +
            "               'colorName', pcr.color, " +
            "               'colorHexCode', '#' || pcr.hex_code, " +
            "               'sizeName', ps.size || ' - ' || ps.description " +
            "           ) " +
            "           ORDER BY pcr.color, " +
            "                    ps.description " +
            "       )::TEXT AS listProductVariants " +
            "FROM product p " +
            "JOIN product_variant pv " +
            "   ON p.id = pv.product_id " +
            "JOIN product_brand pb " +
            "   ON p.product_brand_id = pb.id " +
            queryBrand +
            "JOIN product_category pc " +
            "   ON p.product_category_id = pc.id " +
            queryCategory +
            "JOIN product_color pcr " +
            "   ON pv.color = pcr.id " +
            queryColor +
            "JOIN product_size ps " +
            "   ON pv.\"size\" = ps.\"size\" " +
            querySize +
            queryCursor +
            querySearch +
            "GROUP BY p.id, " +
            "         p.name, " +
            "         p.description, " +
            "         p.base_price, " +
            "         p.\"version\", " +
            "         pb.name, " +
            "         pc.category " +
            "ORDER BY p.id DESC " +
            "LIMIT :limit ";

        Query query = entityManager.createNativeQuery(queryString);
        if (brandId != null) query.setParameter("brandId", brandId);
        if (categoryId != null) query.setParameter("categoryId", categoryId);
        if (colorId != null) query.setParameter("colorId", colorId);
        if (size != null) query.setParameter("size", size);
        if (cursor != null) query.setParameter("cursor", cursor);
        if (search != null) query.setParameter("search", "%" + search + "%");
        query.setParameter("limit", limit);

        List<Object[]> listResult = query.getResultList();

        return listResult.stream().map(
            result -> {
                List<ProductTableVariantDto> listProductVariant =
                    objectMapper.readValue(
                        (String) result[7],
                        new TypeReference<List<ProductTableVariantDto>>() {}
                    );

                return new ProductTableDto(
                        (UUID) result[0],
                        (String) result[1],
                        (String) result[2],
                        (BigDecimal) result[3],
                        Long.valueOf((Integer) result[4]),
                        (String) result[5],
                        (String) result[6],
                        listProductVariant
                );
            }
        ).toList();
    }

//    @Override
//    public List<ProductVariantPriceDto> findListIdAndPrice(
//        Set<UUID> listProductVariantIds
//    ) {
//        Query query = entityManager.createNativeQuery(
//            "SELECT pv.id, " +
//                "      COALESCE(pv.price, p.basePrice) AS price " +
//                "FROM Product p " +
//                "JOIN p.listProductVariants pv " +
//                "    ON p.id = pv.productId " +
//                "    AND pv.id IN :listProductVariantIds " +
//                "    AND pv.deletedAt IS NULL "
//        );
//
//        query.setParameter("listProductVariantIds", listProductVariantIds);
//        List<Object[]> listResult = query.getResultList();
//
//        return listResult.stream().map(
//            result -> {
//                return new ProductVariantPriceDto(
//                    (UUID) result[0],
//                    (BigDecimal) result[1]
//                );
//            }
//        ).toList();
//    }

    @Override
    public List<ProductInvalidMasterDataIdDto> checkInvalidMasterDataIds(
        Set<String> listSizeId,
        Set<String> listColorId,
        Set<UUID> listCategoryId,
        Set<UUID> listBrandId
    ) {
        List<String> queryString = new ArrayList<>();

        // Product Color
        queryString.add(
            "SELECT 'color' AS entityType, " +
            "        id " +
            "FROM UNNEST(?::VARCHAR[]) AS colorId(id) " +
            "WHERE NOT EXISTS ( " +
            "   SELECT 1 " +
            "   FROM product_color pc " +
            "   WHERE colorId.id = pc.id " +
            "       AND deleted_at IS NULL " +
            ") "
        );

        // Product Size
        queryString.add(
            "SELECT 'size' AS entityType, " +
            "        id " +
            "FROM UNNEST(?::VARCHAR[]) AS sizeId(id) " +
            "WHERE NOT EXISTS ( " +
            "   SELECT 1 " +
            "   FROM product_size ps " +
            "   WHERE sizeId.id = ps.\"size\" " +
            "       AND deleted_at IS NULL " +
            ") "
        );

        // Product Category
        queryString.add(
            "SELECT 'category' AS entityType, " +
            "       id::varchar " +
            "FROM UNNEST(?::UUID[]) AS categoryId(id) " +
            "WHERE NOT EXISTS ( " +
            "   SELECT 1 " +
            "   FROM product_category pc " +
            "   WHERE categoryId.id = pc.id " +
            ") "
        );

        // Product Brand
        queryString.add(
            "SELECT 'brand' AS entityType, " +
            "       id::varchar " +
            "FROM UNNEST(?::UUID[]) AS brandId(id) " +
            "WHERE NOT EXISTS ( " +
            "   SELECT 1 " +
            "   FROM product_brand pb " +
            "   WHERE brandId.id = pb.id " +
            "       AND deleted_at IS NULL " +
            ") "
        );

        String completeQueryString = String.join("\n UNION ALL \n", queryString);
        System.out.println(completeQueryString);

        Query query = entityManager.createNativeQuery(completeQueryString);
        query.setParameter(1, listColorId.toArray(new String[0]));
        query.setParameter(2, listSizeId.toArray(new String[0]));
        query.setParameter(3, listCategoryId.toArray(new UUID[0]));
        query.setParameter(4, listBrandId.toArray(new UUID[0]));

        List<Object[]> listResult = query.getResultList();

        return listResult.stream().map(
                result -> {
                    return new ProductInvalidMasterDataIdDto(
                        (String) result[0],
                        (String) result[1]
                    );
                }
        ).toList();
    }

//    @Override
//    @Transactional
//    public List<ProductBulkInsertReturningDto> insertBulkProducts(
//        String[] listProductName,
//        String[] listProductDescription,
//        UUID[] listProductCategoryId,
//        UUID[] listProductBrandId,
//        BigDecimal[] listProductBasePrice
//    ) {
//        Query queryInsertProduct = entityManager.createNativeQuery(
//            "WITH incoming ( " +
//            "   name, description, product_category_id, " +
//            "   product_brand_id, base_price " +
//            ") AS ( " +
//            "   SELECT * " +
//            "   FROM UNNEST( " +
//            "       ?::VARCHAR[], ?::VARCHAR[], ?::UUID[], " +
//            "       ?::UUID[], ?::NUMERIC[] " +
//            "   ) " +
//            "), " +
//            "inserted AS ( " +
//            "   INSERT INTO product ( " +
//            "       name, description, product_category_id, " +
//            "       product_brand_id, base_price, version, created_at " +
//            "   ) " +
//            "   SELECT name, " +
//            "          description, " +
//            "          product_category_id, " +
//            "          product_brand_id, " +
//            "          base_price, " +
//            "          0, " +
//            "          CURRENT_DATE " +
//            "   FROM incoming " +
//            "   ON CONFLICT (name, product_brand_id) DO NOTHING " +
//            "   RETURNING id, name, product_brand_id " +
//            "), " +
//            "conflicted AS ( " +
//            "   SELECT p.id, p.name, p.product_brand_id " +
//            "   FROM product p " +
//            "   JOIN incoming i " +
//            "     ON p.name = i.name " +
//            "     AND p.product_brand_id = i.product_brand_id " +
//            ") " +
//            "SELECT id, name, product_brand_id FROM inserted " +
//            "UNION ALL " +
//            "SELECT id, name, product_brand_id FROM conflicted "
//        );
//
//        queryInsertProduct.setParameter(
//            1,
//            listProductName
//        );
//        queryInsertProduct.setParameter(
//            2,
//            listProductDescription
//        );
//        queryInsertProduct.setParameter(
//            3,
//            listProductCategoryId
//        );
//        queryInsertProduct.setParameter(
//            4,
//            listProductBrandId
//        );
//        queryInsertProduct.setParameter(
//            5,
//            listProductBasePrice
//        );
//
//        List<Object[]> listReturnedProduct = queryInsertProduct.getResultList();
//        List<ProductBulkInsertReturningDto> listMappedReturnedProduct = new ArrayList<>();
//
//        for (Object[] currentReturnedProduct : listReturnedProduct) {
//            listMappedReturnedProduct.add(
//                new ProductBulkInsertReturningDto(
//                    (UUID) currentReturnedProduct[0],
//                    (String) currentReturnedProduct[1],
//                    (UUID) currentReturnedProduct[2]
//                )
//            );
//        }
//
//        return listMappedReturnedProduct;
//    }
    
    @Override
    @Transactional
    public List<ProductBulkInsertReturningDto> insertBulkProducts(
        String[] listProductName,
        String[] listProductDescription,
        UUID[] listProductCategoryId,
        UUID[] listProductBrandId,
        BigDecimal[] listProductBasePrice
    ) {
        Query queryInsertProduct = entityManager.createNativeQuery(
            "INSERT INTO product ( " +
            "   name, description, product_category_id, " +
            "   product_brand_id, base_price " +
            ") " +
            "SELECT DISTINCT ON (name, product_brand_id) " +
            "       name, description, product_category_id, " +
            "       product_brand_id, base_price " +
            "FROM UNNEST ( " +
            "   ?::VARCHAR[], ?::VARCHAR[], ?::UUID[], " +
            "   ?::UUID[], ?::NUMERIC[] " +
            ") AS batch( " +
            "   name, description, product_category_id, " +
            "   product_brand_id, base_price " +
            ") " +
            "ORDER BY name, product_brand_id " +
            "ON CONFLICT (name, product_brand_id) DO NOTHING "
        );

        queryInsertProduct.setParameter(
            1,
            listProductName
        );
        queryInsertProduct.setParameter(
            2,
            listProductDescription
        );
        queryInsertProduct.setParameter(
            3,
            listProductCategoryId
        );
        queryInsertProduct.setParameter(
            4,
            listProductBrandId
        );
        queryInsertProduct.setParameter(
            5,
            listProductBasePrice
        );

        queryInsertProduct.executeUpdate();

        Query queryGetInsertedProduct = entityManager.createNativeQuery(
          "SELECT DISTINCT ON (p.id)" +
          "          p.id, p.name, p.product_brand_id " +
          "FROM product p " +
          "JOIN UNNEST ( " +
          " ?::VARCHAR[], ?::UUID[]" +
          ") AS batch ( " +
          " name, product_brand_id " +
          ") " +
          " ON p.name = batch.name " +
          " AND p.product_brand_id = batch.product_brand_id "
        );

        queryGetInsertedProduct.setParameter(
            1,
            listProductName
        );
        queryGetInsertedProduct.setParameter(
            2,
            listProductBrandId
        );

        List<Object[]> listReturnedProduct = queryGetInsertedProduct.getResultList();
        List<ProductBulkInsertReturningDto> listMappedReturnedProduct = new ArrayList<>();

        for (Object[] currentReturnedProduct : listReturnedProduct) {
            listMappedReturnedProduct.add(
                new ProductBulkInsertReturningDto(
                    (UUID) currentReturnedProduct[0],
                    (String) currentReturnedProduct[1],
                    (UUID) currentReturnedProduct[2]
                )
            );
        }

        return listMappedReturnedProduct;
    }

    @Override
    public int[][] insertBulkProductVariants(List<ProductVariant> listProductVariant) {
        String queryVariant =
            "INSERT INTO product_variant ( " +
            "   product_id, " +
            "   size, " +
            "   color, " +
            "   sku, " +
            "   price, " +
            "   created_at, " +
            "   version " +
            ") " +
            "VALUES " +
            "(?, ?, ?, ?, ?, CURRENT_TIMESTAMP, 0) ";

        int[][] resultVariant = jdbcTemplate.batchUpdate(
            queryVariant,
            listProductVariant,
            500,
            (ps, data) -> {
                ps.setObject(1, data.getProduct().getId());
                ps.setString(2, data.getProductSize().getSize());
                ps.setString(3, data.getProductColor().getId());
                ps.setString(4, data.generateSku());
                ps.setBigDecimal(5, data.getPrice());
            }
        );

        String queryInventory =
            "INSERT INTO inventory ( " +
            "   product_variant_id, " +
            "   quantity, " +
            "   reserved_quantity " +
            ") " +
            "SELECT id, " +
            "       0, " +
            "       0 " +
            "FROM product_variant " +
            "WHERE product_id = ? " +
            "   AND size = ? " +
            "   AND color = ? ";

        jdbcTemplate.batchUpdate(
            queryInventory,
            listProductVariant,
            500,
            (ps, data) -> {
                ps.setObject(1, data.getProduct().getId());
                ps.setString(2, data.getProductSize().getSize());
                ps.setString(3, data.getProductColor().getId());
            }
        );

        return resultVariant;
    }
}
