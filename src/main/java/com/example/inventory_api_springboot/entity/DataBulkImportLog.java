package com.example.inventory_api_springboot.entity;

import com.example.inventory_api_springboot.enums.DataBulkImportLogStatusEnum;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Generated;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "data_bulk_import_log")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DataBulkImportLog {
    @Id
    @Column(insertable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "file_key", updatable = false, nullable = false)
    private String fileKey;

    @Column(nullable = false)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    private DataBulkImportLogStatusEnum status;

    @Column(name = "total_raw_chunk")
    private int totalRawChunk;

    @Column(name = "total_actual_chunk", nullable = false)
    private int totalActualChunk;

    @Column(name = "processed_actual_chunk", nullable = false)
    private int processedActualChunk;

    @Column(name = "max_chunk_size", nullable = false)
    private Integer maxChunkSize;

    @CreatedDate
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", insertable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "dataBulkImportLog", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<DataBulkImportLogError> listDataBulkImportLogError;

    @ManyToOne
    @JoinColumn(name = "created_by")
    private User user;
}
