package com.example.inventory_api_springboot.repository;

import com.example.inventory_api_springboot.entity.StockOpnameReview;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;
import java.util.UUID;

public interface StockOpnameReviewRepository extends JpaRepository<StockOpnameReview, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<StockOpnameReview> findForUpdateById(UUID id);
}
