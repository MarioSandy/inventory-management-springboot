package com.example.inventory_api_springboot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;
import java.util.UUID;

@Embeddable
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DataBulkImportLogErrorId {
    @Column(name = "data_bulk_import_log_id", updatable = false, nullable = false)
    private UUID dataBulkImportLogId;

    @Column(name = "row_number", updatable = false, nullable = false)
    private int rowNumber;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataBulkImportLogErrorId that = (DataBulkImportLogErrorId) o;
        return Objects.equals(dataBulkImportLogId, that.dataBulkImportLogId) && Objects.equals(rowNumber, that.rowNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dataBulkImportLogId, rowNumber);
    }
}
