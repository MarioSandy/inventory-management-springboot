package com.example.inventory_api_springboot.controller;

import com.example.inventory_api_springboot.annotations.RequireRole;
import com.example.inventory_api_springboot.dto.request.ApproveStockOpnameReviewDto;
import com.example.inventory_api_springboot.dto.request.InventoryDamageAdjustmentRequestDto;
import com.example.inventory_api_springboot.dto.request.InventoryRestockRequestDto;
import com.example.inventory_api_springboot.dto.request.RejectStockOpnameReviewDto;
import com.example.inventory_api_springboot.enums.InventoryLogTypeEnum;
import com.example.inventory_api_springboot.enums.UserRoleEnum;
import com.example.inventory_api_springboot.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Controller
@RequestMapping("inventory")
@RequiredArgsConstructor
public class InventoryController {
    private final InventoryService inventoryService;

    @PostMapping("{id}/restock")
    @RequireRole(UserRoleEnum.WAREHOUSE_STAFF)
    public ResponseEntity<String> restock(
        @PathVariable("id") UUID id,
        @Valid @RequestBody InventoryRestockRequestDto restockData,
        @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return inventoryService.restock(
            id,
            restockData,
            userId
        );
    }

    @PostMapping("{id}/damage-adjustment")
    @RequireRole(UserRoleEnum.WAREHOUSE_STAFF)
    public ResponseEntity<String> damageAdjustment(
        @PathVariable("id")
        UUID id,
        @Valid @RequestBody InventoryDamageAdjustmentRequestDto damageAdjustmentData,
        @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return inventoryService.damageAdjustment(
            id,
            damageAdjustmentData,
            userId
        );
    }
}
