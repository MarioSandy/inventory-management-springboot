package com.example.inventory_api_springboot.seeder;

import com.example.inventory_api_springboot.entity.ProductSize;
import com.example.inventory_api_springboot.repository.ProductSizeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductSizeSeeder {
    private final ProductSizeRepository productSizeRepository;

    private List<ProductSize> seederDataMapping() {
        List<ProductSize> listProductSize = List.of(
            ProductSize.builder()
                    .size("XS")
                    .description("Extra Small")
                    .build(),

            ProductSize.builder()
                    .size("S")
                    .description("Small")
                    .build(),

            ProductSize.builder()
                    .size("M")
                    .description("Medium")
                    .build(),

            ProductSize.builder()
                    .size("L")
                    .description("Large")
                    .build(),

            ProductSize.builder()
                    .size("XL")
                    .description("Extra Large")
                    .build(),

            ProductSize.builder()
                    .size("XXL")
                    .description("Double Extra Large")
                    .build(),

            ProductSize.builder()
                    .size("28")
                    .description("Waist 28 inches")
                    .build()
        );

        return listProductSize;
    }

    public Map<String, ProductSize> seedDatabase() {
        return productSizeRepository.saveAll(seederDataMapping())
                .stream()
                .collect(Collectors.toMap(
                        ProductSize::getSize,
                        Function.identity()
                ));
    }

    public void deleteAll() {
        productSizeRepository.deleteAll();
        productSizeRepository.flush();
    }
}
