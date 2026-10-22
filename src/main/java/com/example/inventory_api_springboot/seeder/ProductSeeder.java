package com.example.inventory_api_springboot.seeder;

import com.example.inventory_api_springboot.entity.Inventory;
import com.example.inventory_api_springboot.entity.Product;
import com.example.inventory_api_springboot.entity.ProductBrand;
import com.example.inventory_api_springboot.entity.ProductCategory;
import com.example.inventory_api_springboot.entity.ProductColor;
import com.example.inventory_api_springboot.entity.ProductSize;
import com.example.inventory_api_springboot.entity.ProductVariant;
import com.example.inventory_api_springboot.repository.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProductSeeder {
    private final ProductRepository productRepository;

    public void seedProducts(
            Map<String, ProductCategory> categories,
            Map<String, ProductBrand> brands,
            Map<String, ProductColor> colors,
            Map<String, ProductSize> sizes
    ) {

        createProduct(
                "Basic Cotton T-Shirt",
                "Regular fit cotton t-shirt",
                "T-Shirts",
                "HM01",
                new BigDecimal("79900"),
                categories,
                brands,
                colors,
                sizes
        );

        createProduct(
                "Oversized Graphic T-Shirt",
                "Oversized graphic cotton t-shirt",
                "T-Shirts",
                "ZARA",
                new BigDecimal("149900"),
                categories,
                brands,
                colors,
                sizes
        );

        createProduct(
                "Airism Crew Neck T-Shirt",
                "Lightweight breathable everyday t-shirt",
                "T-Shirts",
                "UNIQ",
                new BigDecimal("129900"),
                categories,
                brands,
                colors,
                sizes
        );

        createProduct(
                "Slim Fit Oxford Shirt",
                "Slim fit cotton oxford shirt",
                "Shirts",
                "UNIQ",
                new BigDecimal("299900"),
                categories,
                brands,
                colors,
                sizes
        );

        createProduct(
                "Classic Denim Jacket",
                "Classic blue denim jacket",
                "Jackets",
                "LEVI",
                new BigDecimal("899900"),
                categories,
                brands,
                colors,
                sizes
        );

        createProduct(
                "Slim Fit Chino Pants",
                "Stretch cotton slim fit chinos",
                "Pants",
                "UNIQ",
                new BigDecimal("399900"),
                categories,
                brands,
                colors,
                sizes
        );

        createProduct(
                "Classic Straight Jeans",
                "Classic straight fit denim jeans",
                "Jeans",
                "LEVI",
                new BigDecimal("799900"),
                categories,
                brands,
                colors,
                sizes
        );

        createProduct(
                "Essential Pullover Hoodie",
                "Classic cotton pullover hoodie",
                "Hoodies",
                "HM01",
                new BigDecimal("399900"),
                categories,
                brands,
                colors,
                sizes
        );

        createProduct(
                "Classic Knit Sweater",
                "Classic lightweight knit sweater",
                "Sweaters",
                "HM01",
                new BigDecimal("399900"),
                categories,
                brands,
                colors,
                sizes
        );

        createProduct(
                "Running Shorts",
                "Lightweight running shorts",
                "Shorts",
                "NIKE",
                new BigDecimal("399900"),
                categories,
                brands,
                colors,
                sizes
        );
    }

    private void createProduct(
            String name,
            String description,
            String categoryName,
            String brandCode,
            BigDecimal basePrice,
            Map<String, ProductCategory> categories,
            Map<String, ProductBrand> brands,
            Map<String, ProductColor> colors,
            Map<String, ProductSize> sizes
    ) {

        Product product = Product.builder()
                .name(name)
                .description(description)
                .productCategory(categories.get(categoryName))
                .productBrand(brands.get(brandCode))
                .basePrice(basePrice)
                .build();

        List<ProductVariant> variants = new ArrayList<>();

        String[] colorIds = {
                "BLK",
                "WHT",
                "NVY",
                "GRY"
        };

        String[] sizeIds = {
                "S",
                "M",
                "L",
                "XL"
        };

        int skuNumber = 1;

        for (String colorId : colorIds) {

            for (String sizeId : sizeIds) {
                ProductVariant variant = ProductVariant.builder()
                        .product(product)
                        .productColor(colors.get(colorId))
                        .productSize(sizes.get(sizeId))
                        .price(basePrice)
                        .build();
                variant.generateSku();

                Inventory inventory = Inventory.builder()
                        .productVariant(variant)
                        .quantity(0)
                        .reservedQuantity(0)
                        .build();

                variant.setInventory(inventory);
                variants.add(variant);

            }
        }

        product.setListProductVariants(variants);
        productRepository.save(product);
    }

    public void deleteAll() {
        productRepository.deleteAll();
        productRepository.flush();
    }
}
