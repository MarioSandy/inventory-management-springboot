package com.example.inventory_api_springboot.entity;

import com.example.inventory_api_springboot.enums.UserBalanceLogTypeEnum;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "user_balance_log")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserBalanceLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "balance_before", nullable = false, updatable = false)
    private BigDecimal balanceBefore;

    @Column(name = "balance_after", nullable = false, updatable = false)
    private BigDecimal balanceAfter;

    @Column(name = "balance_change", nullable = false, updatable = false)
    private BigDecimal balanceChange;

    @Column(name = "reserved_balance_before", nullable = false, updatable = false)
    private BigDecimal reservedBalanceBefore;

    @Column(name = "reserved_balance_after", nullable = false, updatable = false)
    private BigDecimal reservedBalanceAfter;

    @Column(name = "reserved_balance_change", nullable = false, updatable = false)
    private BigDecimal reservedBalanceChange;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    private UserBalanceLogTypeEnum type;

    @Column(name = "reference_id", nullable = false, updatable = false)
    private UUID referenceId;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
}
