package com.example.inventory_api_springboot.service;

import com.example.inventory_api_springboot.dto.queue.InventoryStockOpnameExcelPayloadDto;
import com.example.inventory_api_springboot.dto.queue.OutboxPublisherMessageDto;
import com.example.inventory_api_springboot.dto.queue.ProductExcelCreatePayloadDto;
import com.example.inventory_api_springboot.dto.request.ProductCreateRequestDto;
import com.example.inventory_api_springboot.dto.request.ProductVariantCreateRequestDto;
import com.example.inventory_api_springboot.entity.DataBulkImportLog;
import com.example.inventory_api_springboot.entity.DataBulkImportLogError;
import com.example.inventory_api_springboot.entity.DataBulkImportLogErrorId;
import com.example.inventory_api_springboot.enums.OutboxEventTypeEnum;
import com.github.pjfanning.xlsx.StreamingReader;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class FileOrchestratorService {
    private final S3Service s3Service;
    private final DataBulkImportLogService dataBulkImportLogService;
    private final OutboxService outboxService;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    private String masterDataLookup(
        Sheet sheet,
        String name,
        int rowNumber,
        int cellIndexLookUp,
        String errorMessage,
        DataBulkImportLog dataBulkImportLogProxy,
        List<DataBulkImportLogError> listDataBulkImportLogError
    ) {
        String lookUpId = null;

        for (Row row : sheet) {
            if (
                row.getRowNum() > 0
                && row.getCell(cellIndexLookUp).getStringCellValue().equals(name)
            ) lookUpId = row.getCell(0).getStringCellValue();
        }

        if (lookUpId == null) {
            DataBulkImportLogErrorId dataBulkImportLogErrorId =
                DataBulkImportLogErrorId.builder()
                    .rowNumber(rowNumber)
                    .build();

            DataBulkImportLogError dataBulkImportLogError =
                DataBulkImportLogError.builder()
                    .dataBulkImportLogErrorId(dataBulkImportLogErrorId)
                    .error(errorMessage)
                    .dataBulkImportLog(
                            dataBulkImportLogProxy
                    )
                    .build();

            listDataBulkImportLogError.add(dataBulkImportLogError);
        }

        return lookUpId;
    }

//    private void pushChunkToOutbox(
//        List<ProductExcelCreatePayloadDto> chunked,
//        UUID entityId,
//        UUID createdBy,
//        UUID dataBulkImportLogId,
//        List<DataBulkImportLogError> listDataBulkImportLogError,
//        AtomicInteger totalChunk
//    ) {
//        if (!chunked.isEmpty()) {
//            outboxService.create(
//                entityId,
//                "Product",
//                OutboxEventTypeEnum.CREATE_PRODUCT_BATCH,
//                objectMapper.valueToTree(chunked),
//                createdBy
//            );
//
//            totalChunk.getAndIncrement();
//        }
//
//        dataBulkImportLogService.incrementTotalRawAndActualChunk(
//            dataBulkImportLogId,
//            !chunked.isEmpty()
//        );
//
//        if (!listDataBulkImportLogError.isEmpty())
//            dataBulkImportLogService.insertBulkDataBulkLogError(listDataBulkImportLogError);
//
//        listDataBulkImportLogError.clear();
//        chunked.clear();
//    }

    private <T>void pushChunkToOutbox(
        List<T> chunked,
        UUID entityId,
        UUID createdBy,
        UUID dataBulkImportLogId,
        List<DataBulkImportLogError> listDataBulkImportLogError,
        AtomicInteger totalChunk
    ) {
        String entity = null;
        OutboxEventTypeEnum outboxEventType = null;
        T firstChunked = chunked.get(0);

        if (firstChunked instanceof ProductExcelCreatePayloadDto) {
            entity = "Product";
            outboxEventType = OutboxEventTypeEnum.CREATE_PRODUCT_BATCH;
        } else if (firstChunked instanceof InventoryStockOpnameExcelPayloadDto) {
            entity = "Inventory";
            outboxEventType = OutboxEventTypeEnum.STOCK_OPNAME;
        }

        if (!chunked.isEmpty()) {
            outboxService.create(
                entityId,
                entity,
                outboxEventType,
                objectMapper.valueToTree(chunked),
                createdBy
            );

            totalChunk.getAndIncrement();
        }

        dataBulkImportLogService.incrementTotalRawAndActualChunk(
            dataBulkImportLogId,
            !chunked.isEmpty()
        );

        if (!listDataBulkImportLogError.isEmpty())
            dataBulkImportLogService.insertBulkDataBulkLogError(listDataBulkImportLogError);

        listDataBulkImportLogError.clear();
        chunked.clear();
    }

    public void chunkCreateBatchProductExcelFile(
        OutboxPublisherMessageDto outboxData
    ) throws Exception {
        DataBulkImportLog dataBulkImportLog =
            dataBulkImportLogService.getPendingDataBulkImportLog(outboxData.getEntityId());

        DataBulkImportLog dataBulkImportLogProxy =
            dataBulkImportLogService.getReferenceById(outboxData.getEntityId());

        List<DataBulkImportLogError> listDataBulkImportLogError = new ArrayList<>();

        List<ProductExcelCreatePayloadDto> chunked = new ArrayList<>();
        int rowsToSkip =
            dataBulkImportLog.getTotalRawChunk() * dataBulkImportLog.getMaxChunkSize();
        final AtomicInteger totalChunk = new AtomicInteger(0);
        int currentRowCount = 0;

        try (
            InputStream s3InputStream = s3Service.getFileStream(dataBulkImportLog.getFileKey());
            Workbook workbook = StreamingReader.builder()
            .rowCacheSize(100)
            .open(s3InputStream);
        ) {
            Sheet productColorSheet = workbook.getSheet("product_color");
            Sheet productSizeSheet = workbook.getSheet("product_size");
            Sheet productCategorySheet = workbook.getSheet("product_category");
            Sheet productBrandSheet = workbook.getSheet("product_brand");
            Sheet productSheet = workbook.getSheet("product");
            final AtomicInteger rawChunkSizeCounter = new AtomicInteger(0);

            for (Row productRow : productSheet) {
                if (productRow.getRowNum() == 0) continue;
                currentRowCount++;
                rawChunkSizeCounter.getAndIncrement();

                if (currentRowCount <= rowsToSkip) continue;

                String productCategoryName = productRow.getCell(2).getStringCellValue();
                String productBrandName = productRow.getCell(3).getStringCellValue();
                String productColorName = productRow.getCell(5).getStringCellValue();
                String productSizeName = productRow.getCell(6).getStringCellValue();

                String productSizeId = null;
                UUID productCategoryId = null;
                UUID productBrandId = null;

                String productColorId = masterDataLookup(
                    productColorSheet,
                    productColorName,
                    productRow.getRowNum(),
                    1,
                    "No product color matches in the master product color sheet",
                    dataBulkImportLogProxy,
                    listDataBulkImportLogError
                );

                if (productColorId != null) {
                    productSizeId = masterDataLookup(
                        productSizeSheet,
                        productSizeName,
                        productRow.getRowNum(),
                        0,
                        "No product size matches in the master product size sheet",
                        dataBulkImportLogProxy,
                        listDataBulkImportLogError
                    );
                }

                if (productSizeId != null) {
                    try {
                        String tempProductCategoryId =
                            masterDataLookup(
                                productCategorySheet,
                                productCategoryName,
                                productRow.getRowNum(),
                                1,
                                "No product category matches in the master product category sheet",
                                dataBulkImportLogProxy,
                                listDataBulkImportLogError
                            );

                        if (tempProductCategoryId != null)
                            productCategoryId = UUID.fromString(tempProductCategoryId);
                    } catch (Exception e) {}
                }

                if (productCategoryId != null) {
                    try  {
                        String tempProductBrandId =
                            masterDataLookup(
                                productBrandSheet,
                                productBrandName,
                                productRow.getRowNum(),
                                1,
                                "No product brand matches in the master product brand sheet",
                                dataBulkImportLogProxy,
                                listDataBulkImportLogError
                            );

                        if (tempProductBrandId != null)
                            productBrandId = UUID.fromString(tempProductBrandId);
                    } catch (Exception e) {}
                }


                if (productBrandId != null) {
                    List<ProductVariantCreateRequestDto> listVariantCreateData =
                        Arrays.asList(
                            new ProductVariantCreateRequestDto(
                                productColorId,
                                productSizeId,
                                BigDecimal.valueOf(
                                    productRow.getCell(7).getNumericCellValue()
                                )
                            )
                        );

                    ProductCreateRequestDto productCreateData = new ProductCreateRequestDto(
                        productRow.getCell(0).getStringCellValue(),
                        productRow.getCell(1).getStringCellValue(),
                        productCategoryId,
                        productBrandId,
                        BigDecimal.valueOf(
                            productRow.getCell(4).getNumericCellValue()
                        ),
                        listVariantCreateData
                    );

                    ProductExcelCreatePayloadDto productExcelData = new ProductExcelCreatePayloadDto(
                        productCreateData,
                        productRow.getRowNum()
                    );

                    chunked.add(productExcelData);
                }

                if (rawChunkSizeCounter.get() == 500) {
                    transactionTemplate.executeWithoutResult(
                        result -> {
                            pushChunkToOutbox(
                                chunked,
                                outboxData.getEntityId(),
                                null,
                                dataBulkImportLog.getId(),
                                listDataBulkImportLogError,
                                totalChunk
                            );
                        }
                    );

                    rawChunkSizeCounter.set(0);
                }
            }

            transactionTemplate.executeWithoutResult(
                status -> {
                    if (rawChunkSizeCounter.get() > 0) {
                        pushChunkToOutbox(
                                chunked,
                                outboxData.getEntityId(),
                                null,
                                dataBulkImportLog.getId(),
                                listDataBulkImportLogError,
                                totalChunk
                        );
                    }

                    if (totalChunk.get() == 0) dataBulkImportLogService.updateStatusError(outboxData.getEntityId());
                }
            );
        }
    }

    public void chunkInventoryStockOpnameExcelFile(
        OutboxPublisherMessageDto outboxData
    ) throws Exception {
        DataBulkImportLog dataBulkImportLog =
            dataBulkImportLogService.getPendingDataBulkImportLog(outboxData.getEntityId());

        DataBulkImportLog dataBulkImportLogProxy =
            dataBulkImportLogService.getReferenceById(outboxData.getEntityId());

        List<DataBulkImportLogError> listDataBulkImportLogError = new ArrayList<>();

        List<InventoryStockOpnameExcelPayloadDto> chunked = new ArrayList<>();
        int rowsToSkip =
            dataBulkImportLog.getTotalRawChunk() * dataBulkImportLog.getMaxChunkSize();
        final AtomicInteger totalChunk = new AtomicInteger(0);
        int currentRowCount = 0;

        try (
            InputStream s3InputStream = s3Service.getFileStream(dataBulkImportLog.getFileKey());
            Workbook workbook = StreamingReader.builder()
            .rowCacheSize(100)
            .open(s3InputStream);
        ) {
            final AtomicInteger rawChunkSizeCounter = new AtomicInteger(0);

            Sheet inventoryStockOpnameSheet = workbook.getSheet("stock_opname");

            for (Row inventoryStockOpnameRow : inventoryStockOpnameSheet) {
                if (inventoryStockOpnameRow.getRowNum() == 0) continue;

                currentRowCount++;
                rawChunkSizeCounter.getAndIncrement();

                if (currentRowCount <= rowsToSkip) continue;

                UUID inventoryId =
                    UUID.fromString(inventoryStockOpnameRow.getCell(0).getStringCellValue());
                int actualQuantity = (int) inventoryStockOpnameRow.getCell(5).getNumericCellValue();

                chunked.add(
                    new InventoryStockOpnameExcelPayloadDto(
                        inventoryId,
                        actualQuantity,
                        inventoryStockOpnameRow.getRowNum()
                    )
                );

                if (rawChunkSizeCounter.get() == 100) {
                    transactionTemplate.executeWithoutResult(
                        result -> {
                            pushChunkToOutbox(
                                chunked,
                                outboxData.getEntityId(),
                                null,
                                dataBulkImportLog.getId(),
                                listDataBulkImportLogError,
                                totalChunk
                            );
                        }
                    );

                    rawChunkSizeCounter.set(0);
                }
            }

            transactionTemplate.executeWithoutResult(
                status -> {
                    if (rawChunkSizeCounter.get() > 0) {
                        pushChunkToOutbox(
                                chunked,
                                outboxData.getEntityId(),
                                null,
                                dataBulkImportLog.getId(),
                                listDataBulkImportLogError,
                                totalChunk
                        );
                    }

                    if (totalChunk.get() == 0) dataBulkImportLogService.updateStatusError(outboxData.getEntityId());
                }
            );
        }
    }
}
