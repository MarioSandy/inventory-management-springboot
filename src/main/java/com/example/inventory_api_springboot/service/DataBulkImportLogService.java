package com.example.inventory_api_springboot.service;

import com.example.inventory_api_springboot.entity.DataBulkImportLog;
import com.example.inventory_api_springboot.entity.DataBulkImportLogError;
import com.example.inventory_api_springboot.enums.DataBulkImportLogStatusEnum;
import com.example.inventory_api_springboot.repository.DataBulkImportLogErrorRepository;
import com.example.inventory_api_springboot.repository.DataBulkImportLogRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DataBulkImportLogService {
    private static final String COUNTER_UPDATE_FAILED_MESSAGE =
        "Bulk import log counter update affected %d row(s) instead of 1 for id %s.";

    private final DataBulkImportLogRepository dataBulkImportLogRepository;
    private final DataBulkImportLogErrorRepository dataBulkImportLogErrorRepository;

    public DataBulkImportLog getReferenceById(UUID id) {
        return dataBulkImportLogRepository.getReferenceById(id);
    }

    @Transactional
    public DataBulkImportLog getPendingDataBulkImportLog(
        UUID dataBulkImportLogId
    ) {
        return dataBulkImportLogRepository.findByIdAndStatus(
            dataBulkImportLogId,
            DataBulkImportLogStatusEnum.PENDING
        )
        .orElseThrow(
            () -> new EntityNotFoundException()
        );
    }

    @Transactional
    public UUID insertAndFlushProductBatchCreateLog(
        DataBulkImportLog dataBulkImportLog
    ) {
        DataBulkImportLog savedDataBulkImportLog = dataBulkImportLogRepository.saveAndFlush(dataBulkImportLog);
        return dataBulkImportLog.getId();
    }

    @Transactional
    public void insertBulkDataBulkLogError(List<DataBulkImportLogError> dataBulkImportLogError) {
        dataBulkImportLogErrorRepository.saveAll(dataBulkImportLogError);
    }

    @Transactional
    public void incrementTotalRawAndActualChunk(UUID dataBulkImportLogId, boolean incrementActualChunk) {
        int updatedRows = dataBulkImportLogRepository
            .incrementTotalRawAndActualChunk(dataBulkImportLogId, incrementActualChunk);
        assertSingleRowUpdated(updatedRows, dataBulkImportLogId);
    }

    @Transactional
    public void incrementProcessedChunk(UUID dataBulkImportLogId) {
        int updatedRows = dataBulkImportLogRepository.incrementProcessedActualChunk(dataBulkImportLogId);
        assertSingleRowUpdated(updatedRows, dataBulkImportLogId);
    }

    @Transactional
    public void updateStatusError(UUID dataBulkImportLogId) {
        int updatedRows = dataBulkImportLogRepository.updateStatusError(dataBulkImportLogId);
        assertSingleRowUpdated(updatedRows, dataBulkImportLogId);
    }

    @Transactional
    public boolean updateStatusFinish(UUID dataBulkImportLogId) {
        return dataBulkImportLogRepository.updateStatusFinish(dataBulkImportLogId) == 1;
    }

    private void assertSingleRowUpdated(int updatedRows, UUID dataBulkImportLogId) {
        if (updatedRows != 1) {
            throw new IllegalStateException(
                String.format(COUNTER_UPDATE_FAILED_MESSAGE, updatedRows, dataBulkImportLogId)
            );
        }
    }
}
