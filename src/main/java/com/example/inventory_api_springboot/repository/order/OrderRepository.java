package com.example.inventory_api_springboot.repository.order;

import com.example.inventory_api_springboot.entity.Order;
import com.example.inventory_api_springboot.enums.OrderStatusEnum;
import com.example.inventory_api_springboot.repository.order.OrderNativeRepository;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID>, OrderNativeRepository {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Order> findById(UUID id);

//    @Modifying
//    @Query(
//        "UPDATE Order " +
//        "   SET status = 'CANCELLED' " +
//        "WHERE id = :id "
//    )
//    void updateOrderStatusToCancelled(@Param("id") UUID id);
//
//    @Modifying
//    @Query(
//        "UPDATE Order " +
//        "   SET status = 'RESERVED' " +
//        "WHERE id = :id "
//    )
//    void updateOrderStatusToReserved(@Param("id") UUID id);

    @Modifying
    @Query(
        "UPDATE Order " +
        "   SET status = :status " +
        "WHERE id = :id "
    )
    void updateOrderStatus(@Param("id") UUID id, @Param("status") OrderStatusEnum status);

//    @Lock(LockModeType.PESSIMISTIC_WRITE)
//    @Query(
//        "SELECT DISTINCT o " +
//        "FROM Order o " +
//        "JOIN FETCH o.listOrderDetails od " +
//        "WHERE o.id = :id " +
//        "   AND o.status = 'PROCESSING' " +
//        "   AND NOT EXISTS ( " +
//        "       SELECT 1 " +
//        "       FROM OrderDetail odSub " +
//        "       JOIN Inventory i " +
//        "           ON odSub.productVariant.id = i.productVariant.id " +
//        "       WHERE odSub.order = o " +
//        "           AND i.freeze = true "
//    )
//    Optional<Order> getReadyProcessOrder(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({
        @QueryHint(
            name = "jakarta.persistence.lock.timeout",
            value = "-2"
        )
    })
    Optional<Order> findByIdAndStatus(UUID id, OrderStatusEnum status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({
        @QueryHint(
            name = "jakarta.persistence.lock.timeout",
            value = "-2"
        )
    })
    @EntityGraph(attributePaths = { "listOrderDetails" })
    Optional<Order> findWithDetailsByIdAndStatus(UUID id, OrderStatusEnum status);
}
