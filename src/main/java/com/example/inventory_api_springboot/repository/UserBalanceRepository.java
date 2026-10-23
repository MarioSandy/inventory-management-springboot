package com.example.inventory_api_springboot.repository;

import com.example.inventory_api_springboot.entity.UserBalance;
import com.example.inventory_api_springboot.projection.UserBalanceCardProjection;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface UserBalanceRepository extends JpaRepository<UserBalance, UUID> {
    @Transactional
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({
        @QueryHint(
            name = "jakarta.persistence.lock.timeout",
            value = "-2"
        )
    })
    Optional<UserBalance> findWithLockByUserId(UUID id);

    @Transactional
    @Modifying
    @Query(
        value =
            "UPDATE UserBalance " +
            "   SET balance = balance + :balance, " +
            "       reservedBalance = reservedBalance + :reservedBalance, " +
            "       updatedAt = CURRENT_TIMESTAMP " +
            "WHERE userId = :userId " +
            "   AND reservedBalance + :reservedBalance >= 0 " +
            "   AND balance - (reservedBalance + :reservedBalance) + :balance >= 0  "
    )
    int updateBalanceAndReservedBalance(
        @Param("balance") BigDecimal balance,
        @Param("reservedBalance") BigDecimal reservedBalance,
        @Param("userId") UUID userId
    );

    @Transactional
    @Modifying
    @Query(
        value =
            "UPDATE user_balance ub " +
            "   SET balance = ub.balance - ubl.balance_change, " +
            "       reserved_balance = ub.reserved_balance - ubl.reserved_balance_change " +
            "FROM user_balance_log ubl " +
            "WHERE ub.user_id = ubl.user_id " +
            "   AND ubl.reference_id = :referenceId " +
            "   AND ubl.type = 'ORDER_CANCELLED' " +
            "   AND ub.balance - ubl.balance >= 0 " +
            "   AND ub.reserved_balance - ubl.reserved_balance >= 0 " +
            "   AND NOT EXISTS (" +
            "       SELECT 1 " +
            "       FROM user_balance_log ubl2 " +
            "       WHERE ubl2.reference_id = ubl.reference_id " +
            "           AND ubl2.type IN ('ORDER_RESERVED', 'ORDER_SHIPPED') " +
            "   ) ",
        nativeQuery = true
    )
    int cancelOrder(@Param("referenceId") UUID referenceId);

    @Transactional
    @Modifying
    @Query(
        value =
            "UPDATE user_balance ub " +
            "   SET balance = ub.balance - ubl.reserved_balance_after, " +
            "       reserved_balance = ub.reserved_balance - ubl.reserved_balance_after " +
            "FROM user_balance_log ubl " +
            "WHERE ub.user_id = ubl.user_id " +
            "   AND ubl.reference_id = :referenceId " +
            "   AND ubl.type = 'ORDER_RESERVED' " +
            "   AND ub.balance - ubl.reserved_balance_after >= 0 " +
            "   AND ub.reserved_balance - ubl.reserved_balance_after >= 0 " +
            "   AND NOT EXISTS (" +
            "       SELECT 1 " +
            "       FROM user_balance_log ubl2 " +
            "       WHERE ubl2.reference_id = ubl.reference_id " +
            "           AND ubl2.type IN ('ORDER_CANCELLED', 'ORDER_SHIPPED') " +
            "   ) ",
        nativeQuery = true
    )
    int finishOrder(@Param("referenceId") UUID referenceId);

    Optional<UserBalanceCardProjection> getUserBalanceCardByUserId(UUID userId);
}
