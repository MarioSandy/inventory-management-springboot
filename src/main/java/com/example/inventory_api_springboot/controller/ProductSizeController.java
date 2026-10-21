package com.example.inventory_api_springboot.controller;

import com.example.inventory_api_springboot.dto.request.ProductSizeCreateRequestDto;
import com.example.inventory_api_springboot.projection.ProductSizeDropdownProjection;
import com.example.inventory_api_springboot.service.ProductSizeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("size")
@RequiredArgsConstructor
public class ProductSizeController {
    private final ProductSizeService productSizeService;

    @PostMapping("create")
    public ResponseEntity createProductSize(
        @Valid @RequestBody ProductSizeCreateRequestDto productSizeCreateRequestData
    ) {
        return productSizeService.createProductSize(productSizeCreateRequestData);
    }

    @GetMapping("")
    public ResponseEntity<List<ProductSizeDropdownProjection>> getProductSize(
        @RequestParam String search
    ) {
        return productSizeService.getListProductSize(search);
    }

    @PatchMapping("{id}/restore")
    public ResponseEntity restoreDeletedProductCategory(
        @PathVariable("id")
        @Size(max = 4, message = "Product size cannot be more than 4 characters long")
        String id
    ) {
        return productSizeService.restoreDeletedProductSize(id);
    }

    @DeleteMapping("{id}")
    public ResponseEntity deleteProductSize(
        @PathVariable("id")
        @Size(max = 4, message = "Product size cannot be more than 4 characters long")
        String id
    ) {
        return productSizeService.deleteProductSize(id);
    }
}
