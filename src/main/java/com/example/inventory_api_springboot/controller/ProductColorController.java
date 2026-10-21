package com.example.inventory_api_springboot.controller;

import com.example.inventory_api_springboot.dto.request.ProductColorCreateRequestDto;
import com.example.inventory_api_springboot.projection.ProductColorDropdownProjection;
import com.example.inventory_api_springboot.service.ProductColorService;
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
@RequestMapping("color")
@RequiredArgsConstructor
public class ProductColorController {
    private final ProductColorService productColorService;

    @PostMapping("create")
    public ResponseEntity createProductColor(
        @Valid @RequestBody ProductColorCreateRequestDto productColorCreateRequestData
    ) {
        return productColorService.createProductColor(productColorCreateRequestData);
    }

    @GetMapping("")
    public ResponseEntity<List<ProductColorDropdownProjection>> getListProductColor(
        @RequestParam String search
    ) {
        return productColorService.getListProductColor(search);
    }

    @PatchMapping("{id}/restore")
    public ResponseEntity restoreDeletedProductColor(
        @PathVariable("id")
        @Size(max = 5, message = "Product color id cannot be more than 5 characters")
        String id
    ) {
        return productColorService.restoreDeletedProductColor(id);
    }

    @DeleteMapping("{id}")
    public ResponseEntity deleteProductColor(
        @PathVariable("id")
        @Size(max = 5, message = "Product color id cannot be more than 5 characters")
        String id
    ) {
        return productColorService.deleteProductColor(id);
    }
}
