package com.example.inventory_api_springboot.seeder;

import com.example.inventory_api_springboot.entity.ProductBrand;
import com.example.inventory_api_springboot.entity.ProductCategory;
import com.example.inventory_api_springboot.entity.ProductColor;
import com.example.inventory_api_springboot.entity.ProductSize;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class CLISeeder implements CommandLineRunner {

    private final ProductBrandSeeder productBrandSeeder;
    private final ProductColorSeeder productColorSeeder;
    private final ProductSizeSeeder productSizeSeeder;
    private final ProductCategorySeeder productCategorySeeder;
    private final ProductSeeder productSeeder;
    private final UserSeeder userSeeder;
    private final ApplicationContext applicationContext;

    @Override
    @Transactional
    public void run(String... args) {
        for (String arg : args) {
            if ("--seed".equals(arg)) {
                System.out.println("🚀 Starting manual CLI seed task...");
                // Delete all data
                userSeeder.deleteAll();
                productSeeder.deleteAll();
                productBrandSeeder.deleteAll();
                productCategorySeeder.deleteAll();
                productColorSeeder.deleteAll();
                productSizeSeeder.deleteAll();


                Map<String, ProductColor> productColors = productColorSeeder.seedDatabase();
                Map<String, ProductBrand> productBrands = productBrandSeeder.seedDatabase();
                Map<String, ProductSize> productSizes = productSizeSeeder.seedDatabase();
                Map<String, ProductCategory> productCategories = productCategorySeeder.seedDatabase();

//                productSeeder.seedProducts(
//                    productCategories,
//                    productBrands,
//                    productColors,
//                    productSizes
//                );

                userSeeder.seedDatabase();

                System.out.println("Database seeded successfully.");

//                System.exit(
//                        SpringApplication.exit(applicationContext)
//                );
                break; // Stop looking for other arguments
            }
        }
    }
}
