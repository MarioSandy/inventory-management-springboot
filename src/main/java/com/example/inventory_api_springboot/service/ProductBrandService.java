package com.example.inventory_api_springboot.service;

import com.example.inventory_api_springboot.dto.request.ProductBrandCreateRequestDto;
import com.example.inventory_api_springboot.entity.ProductBrand;
import com.example.inventory_api_springboot.projection.ProductBrandDropdownProjection;
import com.example.inventory_api_springboot.repository.ProductBrandRepository;
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
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductBrandService {
    private final ProductBrandRepository productBrandRepository;

    @Transactional(readOnly = true)
    public ProductBrand getReferenceById(UUID id) {
        return productBrandRepository.getReferenceById(id);
    }

    public ResponseEntity createProductBrand(ProductBrandCreateRequestDto productBrandCreateRequestData) {
        ProductBrand productBrand =
                ProductBrand.builder()
                        .name(productBrandCreateRequestData.name())
                        .description(productBrandCreateRequestData.description())
                        .build();

        productBrandRepository.save(productBrand);
        return ResponseEntity.ok("Product brand created successfully");
    }

    public ResponseEntity restoreDeletedProductBrand(String id) {
        if (productBrandRepository.restoreById(id) == 1) {
            return ResponseEntity.ok("Product brand restored successfully");
        }
        throw new ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "Restore fail. Product brand is not found or already restored"
        );
    }

    public List<ProductBrandDropdownProjection> getDropdown(String search) {
        return productBrandRepository.findByNameContainingIgnoreCase(
                search,
                PageRequest.of(0, 10, Sort.by("name").ascending())
        );
    }

    public ResponseEntity<List<ProductBrandDropdownProjection>> getListProductBrand(String search) {
        List<ProductBrandDropdownProjection> listProductBrand = getDropdown(search);
        return ResponseEntity.ok(listProductBrand);
    }

    public ResponseEntity deleteProductBrand(String id) {
        ProductBrand productBrand =
            productBrandRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product brand not found"));

        productBrand.setDeletedAt(LocalDateTime.now());
        productBrandRepository.save(productBrand);
        return ResponseEntity.ok("Product brand deleted successfully");
    }
}
