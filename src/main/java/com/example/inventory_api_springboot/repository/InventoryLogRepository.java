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
                "       il.quantity_after, " +
                "       il.quantity_before, " +
                "       0, " +
                "       il.reserved_quantity_after, " +
                "       il.reserved_quantity_before, " +
                "       il.reserved_quantity_change * -1, " +
                "       CAST(:#{#type.name()} AS inventory_log_type_enum), " +
                "       :userId, " +
                "       il.reference_id, " +
                "       CURRENT_TIMESTAMP " +
                "FROM inventory_log il " +
                "WHERE il.reference_id = :referenceId " +
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
        @Param("referenceId") UUID referenceId,
        @Param("type") InventoryLogTypeEnum type,
        @Param("userId") UUID userId
    );
}
