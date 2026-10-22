package com.example.inventory_api_springboot.service;

import com.example.inventory_api_springboot.dto.request.ProductColorCreateRequestDto;
import com.example.inventory_api_springboot.entity.ProductColor;
import com.example.inventory_api_springboot.projection.ProductColorDropdownProjection;
import com.example.inventory_api_springboot.repository.ProductColorRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductColorService {
    private final ProductColorRepository productColorRepository;

    @Transactional(readOnly = true)
    public ProductColor getReferenceById(String id) {
        return productColorRepository.getReferenceById(id);
    }

    public ResponseEntity createProductColor(ProductColorCreateRequestDto productColorCreateRequestData) {
        ProductColor productColor =
            ProductColor.builder()
                .id(productColorCreateRequestData.id())
                .color(productColorCreateRequestData.color())
                .hexCode(productColorCreateRequestData.hexCode())
                .build();

        productColorRepository.save(productColor);
        return ResponseEntity.ok("Product color created successfully");
    }

    public List<ProductColorDropdownProjection> getDropdown(String search) {
        return
            productColorRepository.findByColorContainingIgnoreCase(
                    search,
                    PageRequest.of(0, 10, Sort.by("color").ascending())
            );
    }

    public ResponseEntity<List<ProductColorDropdownProjection>> getListProductColor(String search) {
        List<ProductColorDropdownProjection> listProductColor = getDropdown(search);
        return ResponseEntity.ok(listProductColor);
    }

    public ResponseEntity restoreDeletedProductColor(String id) {
        if (productColorRepository.restoreById(id) == 1) {
            return ResponseEntity.ok("Product color restored successfully");
        }
        throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Restore fail. Product color is not found or already restored"
        );
    }

    public ResponseEntity deleteProductColor(String id) {
        ProductColor productColor =
            productColorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product color not found"));

        productColor.setDeletedAt(LocalDateTime.now());
        productColorRepository.save(productColor);
        return ResponseEntity.ok("Product color deleted successfully");
    }
}
