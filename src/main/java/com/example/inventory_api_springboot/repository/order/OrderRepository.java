package com.example.inventory_api_springboot.repository.order;

import com.example.inventory_api_springboot.entity.Order;
import com.example.inventory_api_springboot.enums.OrderStatusEnum;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID>, OrderNativeRepository {
    @Modifying
    @Transactional(propagation = Propagation.MANDATORY)
    @Query(
        "UPDATE Order " +
        "   SET status = :newStatus " +
        "WHERE id = :id " +
        "   AND status = :expectedStatus "
    )
    int updateOrderStatus(
        @Param("id") UUID id,
        @Param("newStatus") OrderStatusEnum newStatus,
        @Param("expectedStatus") OrderStatusEnum expectedStatus
    );
    
    @Transactional(propagation = Propagation.MANDATORY)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Order> findByIdAndStatus(UUID id, OrderStatusEnum status);
}
