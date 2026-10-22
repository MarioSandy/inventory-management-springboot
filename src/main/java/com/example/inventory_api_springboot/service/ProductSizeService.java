package com.example.inventory_api_springboot.service;

import com.example.inventory_api_springboot.dto.request.ProductSizeCreateRequestDto;
import com.example.inventory_api_springboot.entity.ProductSize;
import com.example.inventory_api_springboot.projection.ProductSizeDropdownProjection;
import com.example.inventory_api_springboot.repository.ProductSizeRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
public class ProductSizeService {
    private final ProductSizeRepository productSizeRepository;

    @Transactional(readOnly = true)
    public ProductSize getReferenceById(String id) {
        return productSizeRepository.getReferenceById(id);
    }

    public ResponseEntity createProductSize(ProductSizeCreateRequestDto productSizeCreateRequestData) {
        ProductSize productSize =
            ProductSize.builder()
                .size(productSizeCreateRequestData.size())
                .description(productSizeCreateRequestData.description())
                .build();

        productSizeRepository.save(productSize);
        return ResponseEntity.ok("Product size created successfully");
    }

    public List<ProductSizeDropdownProjection> getDropdown(String search) {
        return productSizeRepository.findBySizeContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                    search,
                    search,
                    PageRequest.of(0, 15, Sort.by("description").ascending())
            );
    }

    public ResponseEntity<List<ProductSizeDropdownProjection>> getListProductSize(String search) {
        List<ProductSizeDropdownProjection> listProductSize = getDropdown(search);
        return ResponseEntity.ok(listProductSize);
    }

    public ResponseEntity restoreDeletedProductSize(String id) {
        if (productSizeRepository.restoreById(id) == 1) {
            return ResponseEntity.ok("Product size restored successfully");
        }
        throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Restore fail. Product size is not found or already restored"
        );
    }

    public ResponseEntity deleteProductSize(String id) {
        ProductSize productSize =
            productSizeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product size not found"));

        productSize.setDeletedAt(LocalDateTime.now());
        productSizeRepository.save(productSize);
        return ResponseEntity.ok("Product size deleted successfully");
    }
}
