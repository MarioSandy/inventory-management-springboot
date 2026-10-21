package com.example.inventory_api_springboot.controller;

import com.example.inventory_api_springboot.dto.request.ProductCreateRequestDto;
import com.example.inventory_api_springboot.dto.request.ProductVariantCreateRequestDto;
import com.example.inventory_api_springboot.entity.Inventory;
import com.example.inventory_api_springboot.entity.Product;
import com.example.inventory_api_springboot.entity.ProductBrand;
import com.example.inventory_api_springboot.entity.ProductCategory;
import com.example.inventory_api_springboot.entity.ProductColor;
import com.example.inventory_api_springboot.entity.ProductSize;
import com.example.inventory_api_springboot.entity.ProductVariant;
import com.example.inventory_api_springboot.repository.product.ProductRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
class ProductControllerCreateTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    // ---------- helpers ----------

    private ProductCategory persistCategory(String category) {
        ProductCategory categoryEntity =
            ProductCategory.builder()
                .category(category)
                .description(category + " description")
                .build();
        entityManager.persist(categoryEntity);
        entityManager.flush();
        return categoryEntity;
    }

    private ProductBrand persistBrand(String code, String name) {
        ProductBrand brand =
            ProductBrand.builder()
                .code(code)
                .name(name)
                .description(name + " description")
                .build();
        entityManager.persist(brand);
        entityManager.flush();
        return brand;
    }

    private ProductColor persistColor(String id, String color, String hexCode) {
        ProductColor colorEntity =
            ProductColor.builder()
                .id(id)
                .color(color)
                .hexCode(hexCode)
                .build();
        entityManager.persist(colorEntity);
        entityManager.flush();
        return colorEntity;
    }

    private ProductSize persistSize(String size, String description) {
        ProductSize sizeEntity =
            ProductSize.builder()
                .size(size)
                .description(description)
                .build();
        entityManager.persist(sizeEntity);
        entityManager.flush();
        return sizeEntity;
    }

    private ProductCreateRequestDto buildRequest(
        String productName,
        UUID categoryId,
        UUID brandId,
        String colorId,
        String sizeId,
        BigDecimal basePrice,
        BigDecimal variantPrice
    ) {
        return new ProductCreateRequestDto(
            productName,
            productName + " description",
            categoryId,
            brandId,
            basePrice,
            List.of(new ProductVariantCreateRequestDto(colorId, sizeId, variantPrice))
        );
    }

    private int statusOf(MvcResult result) {
        return result.getResponse().getStatus();
    }

    // ---------- test #1: happy path ----------

    @Test
    @Transactional
    void createProduct_withWarehouseStaff_persistsProductVariantAndInventory() throws Exception {
        ProductCategory category = persistCategory("HappyCategory");
        ProductBrand brand = persistBrand("HAPPY", "Happy Brand");
        persistColor("HAPPY_BLK", "Happy Black", "111111");
        persistSize("HAPPY_M", "Happy Medium");

        ProductCreateRequestDto request =
            buildRequest(
                "HappyProduct",
                category.getId(),
                brand.getId(),
                "HAPPY_BLK",
                "HAPPY_M",
                new BigDecimal("10000"),
                new BigDecimal("15000")
            );

        mockMvc.perform(
                post("/product/create")
                    .with(
                        jwt()
                            .authorities(new SimpleGrantedAuthority("WAREHOUSE_STAFF"))
                            .claim("role", "WAREHOUSE_STAFF")
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            );

        entityManager.flush();
        entityManager.clear();

        // (a) New product created
        List<Product> products =
            entityManager.createQuery(
                    "SELECT p FROM Product p WHERE p.name = :name", Product.class
                )
                .setParameter("name", "HappyProduct")
                .getResultList();
        assertThat(products).hasSize(1);
        Product savedProduct = products.get(0);

        // (b) New product variants created based on the newly created product
        List<ProductVariant> variants =
            entityManager.createQuery(
                    "SELECT v FROM ProductVariant v WHERE v.product.id = :pid",
                    ProductVariant.class
                )
                .setParameter("pid", savedProduct.getId())
                .getResultList();
        assertThat(variants).hasSize(1);
        ProductVariant savedVariant = variants.get(0);

        // (c) New inventory created with quantity = 0 & reservedQuantity = 0
        Inventory savedInventory = entityManager.find(Inventory.class, savedVariant.getId());
        assertThat(savedInventory).isNotNull();
        assertThat(savedInventory.getQuantity()).isZero();
        assertThat(savedInventory.getReservedQuantity()).isZero();
    }

    // ---------- test #2: role enforcement (non-warehouse) ----------

    @Test
    @Transactional
    void createProduct_withNonWarehouseStaff_isForbidden() throws Exception {
        ProductCreateRequestDto request =
            new ProductCreateRequestDto(
                "ForbiddenProduct",
                "forbidden description",
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("10000"),
                List.of(
                    new ProductVariantCreateRequestDto(
                        "BLK",
                        "M",
                        new BigDecimal("15000")
                    )
                )
            );

        MvcResult result =
            mockMvc.perform(
                    post("/product/create")
                        .with(
                            jwt()
                                .authorities(new SimpleGrantedAuthority("USER_ADMIN"))
                                .claim("role", "USER_ADMIN")
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                .andReturn();

        assertThat(statusOf(result)).isEqualTo(403);
        // Pastikan tidak ada product yang tersimpan
        Long count =
            entityManager.createQuery(
                    "SELECT COUNT(p) FROM Product p WHERE p.name = :name", Long.class
                )
                .setParameter("name", "ForbiddenProduct")
                .getSingleResult();
        assertThat(count).isZero();
    }

    // ---------- test #3: role enforcement (unauthenticated) ----------

    @Test
    @Transactional
    void createProduct_withoutAuthentication_isRejected() throws Exception {
        ProductCreateRequestDto request =
            new ProductCreateRequestDto(
                "AnonProduct",
                "anon description",
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("10000"),
                List.of(
                    new ProductVariantCreateRequestDto(
                        "BLK",
                        "M",
                        new BigDecimal("15000")
                    )
                )
            );

        MvcResult result =
            mockMvc.perform(
                    post("/product/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                .andReturn();

        // OAuth2 resource server -> 401 (atau 403, tergantung entry point)
        assertThat(statusOf(result)).isBetween(401, 403);
    }

    // ---------- test #4: rollback when one of the record cannot be created ----------

    /*
     * Skenario ini sengaja dijalankan DI LUAR transaction test (Propagation.NOT_SUPPORTED)
     * sehingga:
     *   - Setup master data benar-benar commit lewat TransactionTemplate
     *   - Method service benar-benar menyimpan datanya sendiri (tanpa di-join transaksi test)
     *   - Bila salah satu bagian save gagal, transaksi service di-rollback penuh
     *     (Product, ProductVariant, dan Inventory tidak tersisa)
     *
     * Kegagalan disimulasikan dengan memberikan ukuran (size) valid secara format namun
     * tidak ada di DB -> FK constraint pada product_variant.size dilanggar saat flush.
     * Ini meniru kondisi "salah satu record gagal dibuat".
     *
     * @DirtiesContext(methodMode = AFTER_METHOD) me-reset context agar data master yang
     * sudah commit tidak mencemari test lain.
     */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    void createProduct_whenOneRecordCannotBeCreated_rollsBackAllRecordsAndReturnsError() throws Exception {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        UUID categoryId =
            tx.execute(status -> {
                ProductCategory category =
                    ProductCategory.builder()
                        .category("RollbackCategory")
                        .description("rollback description")
                        .build();
                entityManager.persist(category);
                return category.getId();
            });

        UUID brandId =
            tx.execute(status -> {
                ProductBrand brand =
                    ProductBrand.builder()
                        .code("RBCK")
                        .name("RollbackBrand")
                        .description("rollback description")
                        .build();
                entityManager.persist(brand);
                return brand.getId();
            });

        tx.executeWithoutResult(status -> {
            ProductColor color =
                ProductColor.builder()
                    .id("RBCK_BLK")
                    .color("RollbackBlack")
                    .hexCode("999999")
                    .build();
            entityManager.persist(color);
        });

        long productsBefore = productRepository.count();

        ProductCreateRequestDto request =
            buildRequest(
                "RollbackProduct",
                categoryId,
                brandId,
                "RBCK_BLK",
                "SIZE_DOES_NOT_EXIST",
                new BigDecimal("10000"),
                new BigDecimal("15000")
            );

        MvcResult result =
            mockMvc.perform(
                    post("/product/create")
                        .with(
                            jwt()
                                .authorities(new SimpleGrantedAuthority("WAREHOUSE_STAFF"))
                                .claim("role", "WAREHOUSE_STAFF")
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                .andReturn();

        // Response error (bukan 2xx)
        assertThat(statusOf(result))
            .as("Expected error status, but got %d", statusOf(result))
            .isBetween(400, 599);

        // Tidak ada Product / ProductVariant / Inventory yang tersisa
        assertThat(productRepository.count()).isEqualTo(productsBefore);

        Long persistedProducts =
            tx.execute(status ->
                entityManager.createQuery(
                        "SELECT COUNT(p) FROM Product p WHERE p.name = :name", Long.class
                    )
                    .setParameter("name", "RollbackProduct")
                    .getSingleResult()
            );
        assertThat(persistedProducts).isZero();

        Long persistedVariants =
            tx.execute(status ->
                entityManager.createQuery(
                        "SELECT COUNT(v) FROM ProductVariant v WHERE v.product.name = :name",
                        Long.class
                    )
                    .setParameter("name", "RollbackProduct")
                    .getSingleResult()
            );
        assertThat(persistedVariants).isZero();
    }
}
