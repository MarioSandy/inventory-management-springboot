package com.example.inventory_api_springboot.controller;

import com.example.inventory_api_springboot.annotations.RequireRole;
import com.example.inventory_api_springboot.dto.query.response.ProductTableDto;
import com.example.inventory_api_springboot.dto.request.ProductCreateRequestDto;
import com.example.inventory_api_springboot.enums.UserRoleEnum;
import com.example.inventory_api_springboot.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("product")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    @PostMapping("create")
    public ResponseEntity createProduct(
        @Valid @RequestBody ProductCreateRequestDto productCreateRequestData
    ) {
        return productService.createProduct(productCreateRequestData);
    }

    @GetMapping("")
    public ResponseEntity<List<ProductTableDto>> getListProduct(
        @RequestParam(required = false) String search,
        @RequestParam(required = false) String brandId,
        @RequestParam(required = false) String categoryId,
        @RequestParam(required = false) String colorId,
        @RequestParam(required = false) String size,
        @RequestParam(required = false) String cursor,
        @RequestParam int limit
    ) {
        return productService.getListProduct(
            search,
            brandId,
            categoryId,
            colorId,
            size,
            cursor,
            limit
        );
    }

    @GetMapping("create-excel-template")
    @RequireRole(UserRoleEnum.WAREHOUSE_STAFF)
    public ResponseEntity<byte[]> createExcelTemplate() {
        return productService.createExcelTemplate();
    }

    @PostMapping("upload/create-batch")
    @RequireRole(UserRoleEnum.WAREHOUSE_STAFF)
    public ResponseEntity<String> uploadCreateBatch(
        @RequestParam("file") MultipartFile file,
        @AuthenticationPrincipal Jwt jwt
    ) throws IOException {
        return productService.createBatchFromExcel(file, UUID.fromString(jwt.getSubject()));
    }
}
