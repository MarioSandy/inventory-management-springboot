package com.example.inventory_api_springboot.repository;

import com.example.inventory_api_springboot.entity.InventoryLog;
import com.example.inventory_api_springboot.enums.UserBalanceLogTypeEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public interface InventoryLogRepository extends JpaRepository<InventoryLog, Long> {
//    @Transactional
//    @Modifying
//    @Query(
//        value =
//                "INSERT INTO user_balance_log ( " +
//                "   userId, " +
//                "   quantity, " +
//                "   reserved_quantity, " +
//                "   type, " +
//                "   createdBy " +
//                ") " +
//                "SELECT userId, " +
//                "       quantity * -1 AS quantity, " +
//                "       reserved_quantity * -1 AS reserved_quantity, " +
//                "       :type AS type, " +
//                "       :createdBy " +
//                "FROM inventory_log il " +
//                "WHERE reference_id = :referenceId " +
//                "   AND type = 'ORDER_RESERVED'" +
//                "   AND NOT EXISTS (" +
//                "       SELECT 1 " +
//                "       FROM inventory_log il2 " +
//                "       WHERE il2.reference_id = il.reference_id " +
//                "           AND il2.type IN ('ORDER_CANCELLED', 'ORDER_SHIPPED') " +
//                "   ) ",
//        nativeQuery = true
//    )
//    int insertReversedLog(
//            @Param("referenceId") UUID referenceId,
//            @Param("type") UserBalanceLogTypeEnum type,
//            @Param("createdBy") UUID userId
//    );
}
