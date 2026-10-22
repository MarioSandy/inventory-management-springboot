package com.example.inventory_api_springboot.repository;

import com.example.inventory_api_springboot.entity.DataBulkImportLog;
import com.example.inventory_api_springboot.enums.DataBulkImportLogStatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface DataBulkImportLogRepository extends JpaRepository<DataBulkImportLog, UUID> {
    Optional<DataBulkImportLog> findByIdAndStatus(UUID id, DataBulkImportLogStatusEnum status);

    @Modifying
    @Query(
        "UPDATE DataBulkImportLog SET " +
        "   totalRawChunk = totalRawChunk + 1, " +
        "   totalActualChunk = " +
        "       totalActualChunk + ( " +
        "           CASE " +
        "               WHEN :incrementActualChunk = TRUE THEN 1 ELSE 0 " +
        "           END" +
        "       ) " +
        "WHERE id = :id "
    )
    int incrementTotalRawAndActualChunk(@Param("id") UUID id, @Param("incrementActualChunk") boolean incrementActualChunk);

    @Modifying
    @Query(
        "UPDATE DataBulkImportLog " +
        "   SET processedActualChunk = processedActualChunk + 1 " +
        "WHERE id = :id "
    )
    int incrementProcessedActualChunk(@Param("id") UUID id);

    @Modifying
    @Query(
        "UPDATE DataBulkImportLog " +
        "   SET status = 'ERROR' " +
        "WHERE id = :id "
    )
    int updateStatusError(@Param("id") UUID id);

    @Modifying
    @Query(
        "UPDATE DataBulkImportLog " +
        "   SET status = 'FINISH' " +
        "WHERE id = :id " +
        "   AND processedActualChunk = totalActualChunk "
    )
    int updateStatusFinish(@Param("id") UUID id);
}
