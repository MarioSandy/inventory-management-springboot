package com.example.inventory_api_springboot.controller;

import com.example.inventory_api_springboot.annotations.RequireRole;
import com.example.inventory_api_springboot.dto.request.ApproveStockOpnameReviewDto;
import com.example.inventory_api_springboot.dto.request.RejectStockOpnameReviewDto;
import com.example.inventory_api_springboot.enums.UserRoleEnum;
import com.example.inventory_api_springboot.service.StockOpnameService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("stock-opname")
@RequiredArgsConstructor
public class StockOpnameController {
    private final StockOpnameService stockOpnameService;

    @GetMapping("create-excel-template")
    @RequireRole(UserRoleEnum.WAREHOUSE_STAFF)
    public ResponseEntity<byte[]> stockOpnameExcelTemplate() {
        return stockOpnameService.stockOpnameExcelTemplate();
    }

    @PostMapping("upload/inventory-stock-opname")
    @RequireRole(UserRoleEnum.WAREHOUSE_STAFF)
    public ResponseEntity<String> uploadInventoryStockOpname(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal Jwt jwt
    ) throws IOException {
        return stockOpnameService.uploadInventoryStockOpname(
            file,
            UUID.fromString(jwt.getSubject())
        );
    }

    @PostMapping("/review/{id}/approve")
    @RequireRole(UserRoleEnum.WAREHOUSE_STAFF)
    public ResponseEntity<String> approveStockOpnameReview(
            @PathVariable("id") UUID id,
            @Valid @RequestBody ApproveStockOpnameReviewDto approveData,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return stockOpnameService.approveStockOpnameReview(
            approveData,
            id,
            UUID.fromString(jwt.getSubject())
        );
    }

    @PostMapping("/review/{id}/reject")
    @RequireRole(UserRoleEnum.WAREHOUSE_STAFF)
    public ResponseEntity<String> rejectStockOpnameReview(
            @PathVariable("id") UUID id,
            @Valid @RequestBody RejectStockOpnameReviewDto rejectData,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return stockOpnameService.rejectStockOpnameReview(
            rejectData,
            id,
            UUID.fromString(jwt.getSubject())
        );
    }
}
