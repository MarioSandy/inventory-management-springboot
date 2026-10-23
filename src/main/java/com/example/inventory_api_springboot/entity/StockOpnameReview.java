package com.example.inventory_api_springboot.entity;

import com.example.inventory_api_springboot.enums.StockOpnameReviewResolutionEnum;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "stock_opname_review")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StockOpnameReview {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "source_id", nullable = false, updatable = false)
    private UUID sourceId;

    @Column(name = "system_qty", nullable = false, updatable = false)
    int systemQuantity;

    @Column(name = "actual_qty", nullable = false, updatable = false)
    int actualQuantity;

    @Column(name = "system_reserved_qty", nullable = false, updatable = false)
    int systemReservedQuantity;

    @CreatedDate
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "root_cause")
    private String rootCause;

    @Column(name = "investigation_note")
    private String investigationNote;

    @Enumerated(EnumType.STRING)
    @Column(name = "resolution_action")
    @JdbcType(PostgreSQLEnumJdbcType.class)
    private StockOpnameReviewResolutionEnum resolutionAction;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_variant_id", nullable = false)
    private ProductVariant productVariant;

    @ManyToOne
    @JoinColumn(name = "reviewed_by")
    private User user;

//    @OneToMany(mappedBy = "stockOpnameReview", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
//    @ToString.Exclude
//    @EqualsAndHashCode.Exclude
//    private List<InventoryLog> listInventoryLog;
}
