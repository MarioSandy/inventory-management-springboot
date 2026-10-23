package com.example.inventory_api_springboot.repository;

import com.example.inventory_api_springboot.entity.UserBalanceLog;
import com.example.inventory_api_springboot.enums.UserBalanceLogTypeEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface UserBalanceLogRepository extends JpaRepository<UserBalanceLog, UUID> {
    @Query(
        "SELECT SUM(ubl.reservedBalanceChange) " +
        "FROM UserBalanceLog ubl " +
        "WHERE ubl.referenceId = :referenceId " +
        "   AND ubl.type = :type "
    )
    Optional<BigDecimal> sumReservedBalanceChange(
        @Param("referenceId") UUID referenceId,
        @Param("type") UserBalanceLogTypeEnum type
    );

    @Transactional
    @Modifying
    @Query(
        value =
            "INSERT INTO user_balance_log ( " +
            "   user_id, " +
            "   balance_before, " +
            "   balance_after, " +
            "   balance_change, " +
            "   reserved_balance_before, " +
            "   reserved_balance_after, " +
            "   reserved_balance_change, " +
            "   type, " +
            "   reference_id " +
            ") " +
            "SELECT " +
            "   :userId, " +
            "   :balanceBefore, " +
            "   :balanceAfter, " +
            "   CAST(:balanceAfter AS numeric) - CAST(:balanceBefore AS numeric), " +
            "   :reservedBalanceBefore, " +
            "   :reservedBalanceAfter, " +
            "   CAST(:reservedBalanceAfter AS numeric) - CAST(:reservedBalanceBefore AS numeric), " +
            "   CAST('ORDER_CANCELLED' AS user_balance_log_type_enum), " +
            "   :referenceId " +
            "WHERE NOT EXISTS ( " +
            "   SELECT 1 " +
            "   FROM user_balance_log ubl " +
            "   WHERE ubl.reference_id = :referenceId " +
            "       AND ubl.type IN ('ORDER_CANCELLED', 'ORDER_SHIPPED') " +
            ") ",
        nativeQuery = true
    )
    int insertReversedLog(
        @Param("userId") UUID userId,
        @Param("balanceBefore") BigDecimal balanceBefore,
        @Param("balanceAfter") BigDecimal balanceAfter,
        @Param("reservedBalanceBefore") BigDecimal reservedBalanceBefore,
        @Param("reservedBalanceAfter") BigDecimal reservedBalanceAfter,
        @Param("referenceId") UUID referenceId
    );

    @Transactional
    @Modifying
    @Query(
        value =
            "INSERT INTO user_balance_log ( " +
            "   user_id, " +
            "   balance_before, " +
            "   balance_after, " +
            "   balance_change, " +
            "   reserved_balance_before, " +
            "   reserved_balance_after, " +
            "   reserved_balance_change, " +
            "   type, " +
            "   reference_id " +
            ") " +
            "SELECT " +
            "   :userId, " +
            "   :balanceBefore, " +
            "   :balanceAfter, " +
            "   CAST(:balanceAfter AS numeric) - CAST(:balanceBefore AS numeric), " +
            "   :reservedBalanceBefore, " +
            "   :reservedBalanceAfter, " +
            "   CAST(:reservedBalanceAfter AS numeric) - CAST(:reservedBalanceBefore AS numeric), " +
            "   CAST('ORDER_SHIPPED' AS user_balance_log_type_enum), " +
            "   :referenceId " +
            "WHERE NOT EXISTS ( " +
            "   SELECT 1 " +
            "   FROM user_balance_log ubl " +
            "   WHERE ubl.reference_id = :referenceId " +
            "       AND ubl.type IN ('ORDER_CANCELLED', 'ORDER_SHIPPED') " +
            ") ",
        nativeQuery = true
    )
    int insertSwitchedLog(
        @Param("userId") UUID userId,
        @Param("balanceBefore") BigDecimal balanceBefore,
        @Param("balanceAfter") BigDecimal balanceAfter,
        @Param("reservedBalanceBefore") BigDecimal reservedBalanceBefore,
        @Param("reservedBalanceAfter") BigDecimal reservedBalanceAfter,
        @Param("referenceId") UUID referenceId
    );
}
