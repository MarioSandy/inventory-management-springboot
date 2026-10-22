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
        dataBulkImportLogRepository.incrementTotalRawAndActualChunk(dataBulkImportLogId, incrementActualChunk);
    }

    @Transactional
    public void incrementProcessedChunk(UUID dataBulkImportLogId) {
        dataBulkImportLogRepository.incrementProcessedActualChunk(dataBulkImportLogId);
    }

    @Transactional
    public void updateStatusError(UUID dataBulkImportLogId) {
        dataBulkImportLogRepository.updateStatusError(dataBulkImportLogId);
    }

    @Transactional
    public void updateStatusFinish(UUID dataBulkImportLogId) {
        dataBulkImportLogRepository.updateStatusFinish(dataBulkImportLogId);
    }
}
