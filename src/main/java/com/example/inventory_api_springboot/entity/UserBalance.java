package com.example.inventory_api_springboot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "user_balance")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserBalance {
    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(nullable = false)
    private BigDecimal balance;

    @Column(name = "reserved_balance", nullable = false)
    private BigDecimal reservedBalance;

    @LastModifiedDate
    @Column(name = "updated_at", insertable = false)
    private LocalDateTime updatedAt;

    @OneToOne
    @MapsId // 2. Tells Hibernate to copy the 'id' from the linked User entity
    @JoinColumn(name = "user_id") // 3. The column in this table is named 'id' and acts as the FK
    private User user;

    public void mutateBalances(BigDecimal changeBalance, BigDecimal changeReservedBalance) {
        if (this.balance == null || this.reservedBalance == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "User balance state is not initialized (balance and reserved balance must not be null)."
            );
        }

        if (changeBalance == null || changeReservedBalance == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Balance change and reserved balance change must not be null."
            );
        }

        BigDecimal newBalance = this.balance.add(changeBalance);
        BigDecimal newReservedBalance = this.reservedBalance.add(changeReservedBalance);

        if (newBalance.signum() < 0) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                String.format(
                    "Balance cannot be negative. (balanceBefore=%s, balanceChange=%s, balanceAfter=%s)",
                    this.balance.toPlainString(),
                    changeBalance.toPlainString(),
                    newBalance.toPlainString()
                )
            );
        }

        if (newReservedBalance.signum() < 0) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                String.format(
                    "Reserved balance cannot be negative. (reservedBalanceBefore=%s, reservedBalanceChange=%s, reservedBalanceAfter=%s)",
                    this.reservedBalance.toPlainString(),
                    changeReservedBalance.toPlainString(),
                    newReservedBalance.toPlainString()
                )
            );
        }

        BigDecimal availableBalance = newBalance.subtract(newReservedBalance);
        if (availableBalance.signum() < 0) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                String.format(
                    "Insufficient balance! Available balance cannot drop below zero. " +
                    "(balanceAfter=%s, reservedBalanceAfter=%s, availableBalance=%s)",
                    newBalance.toPlainString(),
                    newReservedBalance.toPlainString(),
                    availableBalance.toPlainString()
                )
            );
        }

        this.balance = newBalance;
        this.reservedBalance = newReservedBalance;
    }
}
