package com.example.inventory_api_springboot.seeder;

import com.example.inventory_api_springboot.entity.ProductCategory;
import com.example.inventory_api_springboot.repository.ProductCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductCategorySeeder {
    private final ProductCategoryRepository productCategoryRepository;

    private List<ProductCategory> seederDataMapping() {
        List<ProductCategory> listProductCategories = List.of(
                ProductCategory.builder()
                        .category("T-Shirts")
                        .description("Casual short-sleeve and long-sleeve t-shirts")
                        .build(),

                ProductCategory.builder()
                        .category("Shirts")
                        .description("Casual and formal button-up shirts")
                        .build(),

                ProductCategory.builder()
                        .category("Hoodies")
                        .description("Hooded sweatshirts and casual outerwear")
                        .build(),

                ProductCategory.builder()
                        .category("Jackets")
                        .description("Casual and lightweight jackets")
                        .build(),

                ProductCategory.builder()
                        .category("Pants")
                        .description("Casual and everyday pants")
                        .build(),

                ProductCategory.builder()
                        .category("Jeans")
                        .description("Denim jeans in various fits")
                        .build(),

                ProductCategory.builder()
                        .category("Shorts")
                        .description("Casual and everyday shorts")
                        .build(),

                ProductCategory.builder()
                        .category("Sweaters")
                        .description("Knitted and warm casual sweaters")
                        .build()
        );

        return listProductCategories;
    }


    public Map<String, ProductCategory> seedDatabase() {
        productCategoryRepository.deleteAll();
        return productCategoryRepository.saveAll(seederDataMapping())
                .stream()
                .collect(Collectors.toMap(
                        ProductCategory::getCategory,
                        Function.identity()
                ));
    }

    public void deleteAll() {
        productCategoryRepository.deleteAll();
        productCategoryRepository.flush();
    }
}
