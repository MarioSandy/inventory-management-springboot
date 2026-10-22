package com.example.inventory_api_springboot.seeder;

import com.example.inventory_api_springboot.entity.ProductColor;
import com.example.inventory_api_springboot.repository.ProductColorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductColorSeeder {
    private final ProductColorRepository productColorRepository;

    private List<ProductColor> seederDataMapping() {
        List<ProductColor> listProductColors = List.of(
            ProductColor.builder()
                    .id("BLK")
                    .color("Black")
                    .hexCode("000000")
                    .build(),

            ProductColor.builder()
                    .id("WHT")
                    .color("White")
                    .hexCode("FFFFFF")
                    .build(),

            ProductColor.builder()
                    .id("NVY")
                    .color("Navy")
                    .hexCode("000080")
                    .build(),

            ProductColor.builder()
                    .id("GRY")
                    .color("Gray")
                    .hexCode("808080")
                    .build(),

            ProductColor.builder()
                    .id("RED")
                    .color("Red")
                    .hexCode("FF0000")
                    .build(),

            ProductColor.builder()
                    .id("BLU")
                    .color("Blue")
                    .hexCode("0000FF")
                    .build(),

            ProductColor.builder()
                    .id("GRN")
                    .color("Green")
                    .hexCode("008000")
                    .build(),

            ProductColor.builder()
                    .id("BRN")
                    .color("Brown")
                    .hexCode("A52A2A")
                    .build(),

            ProductColor.builder()
                    .id("BEG")
                    .color("Beige")
                    .hexCode("F5F5DC")
                    .build(),

            ProductColor.builder()
                    .id("PNK")
                    .color("Pink")
                    .hexCode("FFC0CB")
                    .build(),

            ProductColor.builder()
                    .id("CRM")
                    .color("Cream")
                    .hexCode("FFFDD0")
                    .build(),

            ProductColor.builder()
                    .id("OLV")
                    .color("Olive")
                    .hexCode("808000")
                    .build()
        );

        return listProductColors;
    }

    public Map<String, ProductColor> seedDatabase() {
        return productColorRepository.saveAll(seederDataMapping())
                .stream()
                .collect(Collectors.toMap(
                        ProductColor::getId,
                        Function.identity()
                ));
    }

    public void deleteAll() {
        productColorRepository.deleteAll();
        productColorRepository.flush();
    }
}
