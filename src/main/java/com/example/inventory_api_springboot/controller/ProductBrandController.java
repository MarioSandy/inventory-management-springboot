package com.example.inventory_api_springboot.controller;

import com.example.inventory_api_springboot.dto.request.ProductBrandCreateRequestDto;
import com.example.inventory_api_springboot.projection.ProductBrandDropdownProjection;
import com.example.inventory_api_springboot.service.ProductBrandService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("brand")
@RequiredArgsConstructor
@Validated
public class ProductBrandController {
    private final ProductBrandService productBrandService;

    @PostMapping("create")
    public ResponseEntity createProductBrand(
        @Valid @RequestBody ProductBrandCreateRequestDto productBrandCreateRequestData
    ) {
        return productBrandService.createProductBrand(productBrandCreateRequestData);
    }

    @PatchMapping("{id}/restore")
    public ResponseEntity restoreDeletedProductBrand(
        @PathVariable("id")
        @Size(min = 4, max = 4, message = "Brand should be 4 characters long")
        String id
    ) {
        return productBrandService.restoreDeletedProductBrand(id);
    }

    @GetMapping("")
    public ResponseEntity<List<ProductBrandDropdownProjection>> getListProductBrand(
        @RequestParam String search
    ) {
        return productBrandService.getListProductBrand(search);
    }

    @DeleteMapping("{id}")
    public ResponseEntity deleteProductBrand(
        @PathVariable("id")
        @Size(min = 4, max = 4, message = "Brand should be 4 characters long")
        String id
    ) {
        return productBrandService.deleteProductBrand(id);
    }
}
