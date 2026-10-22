package com.example.inventory_api_springboot.service;

import com.example.inventory_api_springboot.dto.request.ProductCategoryCreateRequestDto;
import com.example.inventory_api_springboot.entity.ProductCategory;
import com.example.inventory_api_springboot.projection.ProductCategoryDropdownProjection;
import com.example.inventory_api_springboot.repository.ProductCategoryRepository;
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
public class ProductCategoryService {
    private final ProductCategoryRepository productCategoryRepository;

    @Transactional(readOnly = true)
    public ProductCategory getReferenceById(UUID id) {
        return productCategoryRepository.getReferenceById(id);
    }

    public ResponseEntity createProductCategory(
            ProductCategoryCreateRequestDto productCategoryCreateRequestData
    ) {
        ProductCategory productCategory =
            ProductCategory.builder()
                .category(productCategoryCreateRequestData.category())
                .description(productCategoryCreateRequestData.description())
                .build();

        productCategoryRepository.save(productCategory);
        return ResponseEntity.ok("Product category created successfully");
    }

    public List<ProductCategoryDropdownProjection> getDropdown(String search) {
        return productCategoryRepository.findByCategoryContainingIgnoreCase(
            search,
            PageRequest.of(0, 10, Sort.by("category").ascending())
        );
    }

    public ResponseEntity<List<ProductCategoryDropdownProjection>> getListProductCategory(String search) {
        List<ProductCategoryDropdownProjection> listProductCategory = getDropdown(search);
        return ResponseEntity.ok(listProductCategory);
    }

    public ResponseEntity restoreDeletedProductCategory(UUID id) {
        if (productCategoryRepository.restoreById(id) == 1) {
            return ResponseEntity.ok("Product category restored successfully");
        }
        throw new ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "Restore fail. Product category is not found or already restored"
        );
    }

    public ResponseEntity deleteProductCategory(UUID id) {
        ProductCategory productCategory =
            productCategoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product category is not found"));

        productCategory.setDeletedAt(LocalDateTime.now());
        productCategoryRepository.save(productCategory);
        return ResponseEntity.ok("Product category deleted successfully");
    }
}
