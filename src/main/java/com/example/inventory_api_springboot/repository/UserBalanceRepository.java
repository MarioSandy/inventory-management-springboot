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

    Optional<UserBalanceCardProjection> getUserBalanceCardByUserId(UUID userId);
}
