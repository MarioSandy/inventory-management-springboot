package com.example.inventory_api_springboot.controller;

import com.example.inventory_api_springboot.dto.request.ProductCategoryCreateRequestDto;
import com.example.inventory_api_springboot.projection.ProductCategoryDropdownProjection;
import com.example.inventory_api_springboot.service.ProductCategoryService;
import jakarta.validation.Valid;
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
import java.util.UUID;

@RestController
@RequestMapping("category")
@RequiredArgsConstructor
@Validated
public class ProductCategoryController {
    private final ProductCategoryService productCategoryService;

    @PostMapping("create")
    public ResponseEntity createProductCategory(
        @Valid @RequestBody ProductCategoryCreateRequestDto productCategoryCreateRequestData
    ) {
        return productCategoryService.createProductCategory(productCategoryCreateRequestData);
    }

    @GetMapping("")
    public ResponseEntity<List<ProductCategoryDropdownProjection>> getListProductCategory (
        @RequestParam String search
    ) {
        return productCategoryService.getListProductCategory(search);
    }

    @PatchMapping("{id}/restore")
    public ResponseEntity restoreDeletedProductCategory(
        @PathVariable("id") UUID id
    ) {
        return productCategoryService.restoreDeletedProductCategory(id);
    }

    @DeleteMapping("{id}")
    public ResponseEntity deleteProductCategory(
        @PathVariable("id") UUID id
    ) {
        return productCategoryService.deleteProductCategory(id);
    }
}
