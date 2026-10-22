package com.example.inventory_api_springboot.seeder;

import com.example.inventory_api_springboot.entity.ProductBrand;
import com.example.inventory_api_springboot.repository.ProductBrandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductBrandSeeder {
    private final ProductBrandRepository productBrandRepository;

    private List<ProductBrand> seederDataMapping() {
        List<ProductBrand> listProductBrand = List.of(
            ProductBrand.builder()
                    .code("HM01")
                    .name("H&M")
                    .description("Fashion clothing and accessories")
                    .build(),

            ProductBrand.builder()
                    .code("ZARA")
                    .name("Zara")
                    .description("Contemporary fashion clothing")
                    .build(),

            ProductBrand.builder()
                    .code("UNIQ")
                    .name("Uniqlo")
                    .description("Everyday casual clothing")
                    .build(),

            ProductBrand.builder()
                    .code("LEVI")
                    .name("Levis")
                    .description("Denim and casual clothing")
                    .build(),

            ProductBrand.builder()
                    .code("PUMA")
                    .name("Puma")
                    .description("Sportswear and lifestyle clothing")
                    .build(),

            ProductBrand.builder()
                    .code("NIKE")
                    .name("Nike")
                    .description("Sportswear and athletic clothing")
                    .build()
        );

        return listProductBrand;
    }

    public Map<String, ProductBrand> seedDatabase() {
        return productBrandRepository.saveAll(seederDataMapping())
            .stream()
                .collect(Collectors.toMap(
                    ProductBrand::getCode,
                    Function.identity()
                ));
    }

    public void deleteAll() {
        productBrandRepository.deleteAll();
        productBrandRepository.flush();
    }
}
