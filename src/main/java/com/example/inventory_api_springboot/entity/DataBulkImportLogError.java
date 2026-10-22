package com.example.inventory_api_springboot.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "data_bulk_import_log_error")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DataBulkImportLogError {
    @EmbeddedId
    private DataBulkImportLogErrorId dataBulkImportLogErrorId;
    private String error;

    @ManyToOne()
    @MapsId("dataBulkImportLogId")
    @JoinColumn(name = "data_bulk_import_log_id")
    private DataBulkImportLog dataBulkImportLog;
}
