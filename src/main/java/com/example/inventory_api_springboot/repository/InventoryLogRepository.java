package com.example.inventory_api_springboot.repository;

import com.example.inventory_api_springboot.entity.InventoryLog;
import com.example.inventory_api_springboot.enums.InventoryLogTypeEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public interface InventoryLogRepository extends JpaRepository<InventoryLog, Long> {

    @Transactional
    @Modifying
    @Query(
        value =
            "INSERT INTO inventory_log ( " +
            "   product_variant_id, " +
            "   quantity_before, " +
            "   quantity_after, " +
            "   quantity_change, " +
            "   reserved_quantity_before, " +
            "   reserved_quantity_after, " +
            "   reserved_quantity_change, " +
            "   type, " +
            "   created_by, " +
            "   reference_id, " +
            "   created_at " +
            ") " +
            "SELECT il.product_variant_id, " +
            "       i.quantity + CASE WHEN CAST(:#{#type.name()} AS varchar) = 'ORDER_SHIPPED' THEN il.reserved_quantity_change ELSE 0 END, " +
            "       i.quantity, " +
            "       CASE WHEN CAST(:#{#type.name()} AS varchar) = 'ORDER_SHIPPED' THEN -il.reserved_quantity_change ELSE 0 END, " +
            "       i.reserved_quantity + il.reserved_quantity_change, " +
            "       i.reserved_quantity, " +
            "       -il.reserved_quantity_change, " +
            "       CAST(:#{#type.name()} AS inventory_log_type_enum), " +
            "       :userId, " +
            "       il.reference_id, " +
            "       NOW() " +
            "FROM inventory_log il " +
            "JOIN order_detail od ON od.id = il.reference_id " +
            "JOIN inventory i ON i.product_variant_id = il.product_variant_id " +
            "WHERE od.order_id = :orderId " +
            "   AND il.type = 'ORDER_RESERVED' " +
            "   AND NOT EXISTS (" +
            "       SELECT 1 " +
            "       FROM inventory_log il2 " +
            "       WHERE il2.reference_id = il.reference_id " +
            "           AND il2.type IN ('ORDER_CANCELLED', 'ORDER_SHIPPED') " +
            "   ) ",
        nativeQuery = true
    )
    int insertReversedLog(
        @Param("orderId") UUID orderId,
        @Param("type") InventoryLogTypeEnum type,
        @Param("userId") UUID userId
    );
}
