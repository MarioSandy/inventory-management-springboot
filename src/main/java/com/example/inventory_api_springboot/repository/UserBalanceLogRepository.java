package com.example.inventory_api_springboot.repository;

import com.example.inventory_api_springboot.entity.UserBalanceLog;
import com.example.inventory_api_springboot.enums.UserBalanceLogTypeEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public interface UserBalanceLogRepository extends JpaRepository<UserBalanceLog, UUID> {
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
            "SELECT user_id, " +
            "       balance" +
            "       balance_change * -1 AS balance_change, " +
            "       reserved_balance_change * -1 AS reserved_balance_change, " +
            "       CAST(:#{#type.name()} AS user_balance_log_type_enum) AS type, " +
            "       reference_id " +
            "FROM user_balance_log ubl " +
            "WHERE reference_id = :referenceId " +
            "   AND type = 'ORDER_RESERVED'" +
            "   AND NOT EXISTS (" +
            "       SELECT 1 " +
            "       FROM user_balance_log ubl2 " +
            "       WHERE ubl2.reference_id = ubl.reference_id " +
            "           AND ubl2.type IN ('ORDER_CANCELLED', 'ORDER_SHIPPED') " +
            "   ) ",
        nativeQuery = true
    )
    int insertReversedLog(
        @Param("referenceId") UUID referenceId,
        @Param("type") UserBalanceLogTypeEnum type
    );

    @Transactional
    @Modifying
    @Query(
        value =
            "INSERT INTO user_balance_log ( " +
            "   user_id, " +
            "   balance, " +
            "   reserved_balance, " +
            "   type, " +
            "   reference_id " +
            ") " +
            "SELECT user_id, " +
            "       balance * -1 AS balance, " +
            "       reserved_balance * -1 AS reserved_balance, " +
            "       CAST(:#{#type.name()} AS user_balance_log_type_enum) AS type, " +
            "       reference_id " +
            "FROM user_balance_log ubl " +
            "WHERE reference_id = :referenceId " +
            "   AND type = 'ORDER_RESERVED'" +
            "   AND NOT EXISTS (" +
            "       SELECT 1 " +
            "       FROM user_balance_log ubl2 " +
            "       WHERE ubl2.reference_id = ubl.reference_id " +
            "           AND ubl2.type IN ('ORDER_CANCELLED', 'ORDER_SHIPPED') " +
            "   ) ",
        nativeQuery = true
    )
    int insertSwitchedLog(
        @Param("referenceId") UUID referenceId,
        @Param("type") UserBalanceLogTypeEnum type
    );
}
