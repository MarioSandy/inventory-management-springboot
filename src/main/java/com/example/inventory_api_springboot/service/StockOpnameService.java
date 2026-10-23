package com.example.inventory_api_springboot.service;

import com.example.inventory_api_springboot.dto.query.request.InventoryStockOpnameDto;
import com.example.inventory_api_springboot.dto.query.request.UserBalanceMutationDto;
import com.example.inventory_api_springboot.dto.query.response.InventoryStockOpnameExcelDto;
import com.example.inventory_api_springboot.dto.queue.InventoryStockOpnameExcelPayloadDto;
import com.example.inventory_api_springboot.dto.queue.OutboxPublisherMessageDto;
import com.example.inventory_api_springboot.dto.request.ApproveStockOpnameReviewDto;
import com.example.inventory_api_springboot.dto.request.RejectStockOpnameReviewDto;
import com.example.inventory_api_springboot.dto.response.OrderDetailCancelResponseDto;
import com.example.inventory_api_springboot.entity.DataBulkImportLog;
import com.example.inventory_api_springboot.entity.DataBulkImportLogError;
import com.example.inventory_api_springboot.entity.DataBulkImportLogErrorId;
import com.example.inventory_api_springboot.entity.Inventory;
import com.example.inventory_api_springboot.entity.InventoryLog;
import com.example.inventory_api_springboot.entity.StockOpnameReview;
import com.example.inventory_api_springboot.enums.DataBulkImportLogStatusEnum;
import com.example.inventory_api_springboot.enums.InventoryLogTypeEnum;
import com.example.inventory_api_springboot.enums.OutboxEventTypeEnum;
import com.example.inventory_api_springboot.enums.StockOpnameReviewResolutionEnum;
import com.example.inventory_api_springboot.enums.UserBalanceLogTypeEnum;
import com.example.inventory_api_springboot.repository.StockOpnameReviewRepository;
import com.github.pjfanning.xlsx.StreamingReader;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockOpnameService {
    private final StockOpnameReviewRepository stockOpnameReviewRepository;
    private final UserService userService;
    private final ProductService productService;
    private final UserBalanceService userBalanceService;
    private final S3Service s3Service;
    private final OutboxService outboxService;
    private final DataBulkImportLogService dataBulkImportLogService;
    private final InventoryService inventoryService;
    private final OrderService orderService;
    private final ObjectMapper objectMapper;
    @Value("${spring.file-chunk.max-chunk-size.inventory-stock-opname}")
    private int maxChunkSize;

    @Transactional
    public ResponseEntity<byte[]> stockOpnameExcelTemplate() {
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100)) {
            Sheet sheet = workbook.createSheet("stock_opname");
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            CellStyle unlockedStyle = workbook.createCellStyle();
            unlockedStyle.setLocked(false);

            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Inventory ID");
            header.createCell(1).setCellValue("SKU");
            header.createCell(2).setCellValue("Name");
            header.createCell(3).setCellValue("Quantity");
            header.createCell(4).setCellValue("Reserved Quantity");
            header.createCell(6).setCellValue("Minimum Actual Quantity");

            Cell actualQuantityHeaderCell = header.createCell(5);
            actualQuantityHeaderCell.setCellValue("Actual Quantity");
            actualQuantityHeaderCell.setCellStyle(unlockedStyle);

            UUID cursor = null;
            Pageable pageable = PageRequest.of(0, 2001);
            boolean isNotLast = true;
            int excelRowNumber = 1;

            while(isNotLast) {
                List<InventoryStockOpnameExcelDto> listInventory =
                    inventoryService.getInventoryForStockOpnameExcel(cursor);

                if (listInventory.size() < 2001) {
                    isNotLast = false;
                } else {
                    cursor = listInventory.get(listInventory.size() - 2).getId();
                    listInventory.remove(listInventory.size() - 1);
                }

                for (InventoryStockOpnameExcelDto currentInventory : listInventory) {
                    Row currentRow = sheet.createRow(excelRowNumber++);
                    currentRow.createCell(0).setCellValue(currentInventory.getId().toString());
                    currentRow.createCell(1).setCellValue(currentInventory.getSku());
                    currentRow.createCell(2).setCellValue(currentInventory.getName());
                    currentRow.createCell(3).setCellValue(currentInventory.getQuantity());
                    currentRow.createCell(4).setCellValue(currentInventory.getReservedQuantity());
                    currentRow.createCell(6).setCellValue(currentInventory.getMinimumActualQuantity());

                    Cell actualQuantityCell = currentRow.createCell(5);
                    actualQuantityCell.setCellValue(0);
                    actualQuantityCell.setCellStyle(unlockedStyle);
                }
            }

            sheet.setColumnHidden(0, true);
            sheet.protectSheet(UUID.randomUUID().toString());
            workbook.write(out);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=inventory_stock_opname_template.xlsx")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(out.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Excel template generation failed: " + e.getMessage(), e);
        }
    }

    @Transactional
    public ResponseEntity<String> uploadInventoryStockOpname(
            MultipartFile file,
            UUID userId
    ) throws IOException {
        // Store file to S3
        String key = s3Service.generateS3Key("inventory/stock-opname-from-excel");
        s3Service.uploadFileToS3(key, file.getBytes());

        Set<UUID> listProductVariantId = new HashSet<>();
        try (InputStream inputStream = file.getInputStream()) {
            Workbook workbook = StreamingReader.builder()
                    .rowCacheSize(100)
                    .open(inputStream);

            Sheet sheet = workbook.getSheet("stock_opname");

            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue;

                listProductVariantId.add(
                    UUID.fromString(
                        row.getCell(0).getStringCellValue()
                    )
                );
            }
        }

        // Freeze inventory
        inventoryService.freezeInventories(listProductVariantId);
        // Insert log
        DataBulkImportLog dataBulkImportLog = DataBulkImportLog.builder()
            .fileKey(key)
            .status(DataBulkImportLogStatusEnum.PENDING)
            .maxChunkSize(maxChunkSize)
            .user(userService.getReferenceById(userId))
            .build();

        UUID savedDataBulkImportLogId = dataBulkImportLogService.insertAndFlushProductBatchCreateLog(dataBulkImportLog);

        // Insert process to outbox table
        outboxService.create(
                savedDataBulkImportLogId,
                "DataBulkImportLog",
                OutboxEventTypeEnum.FILE_ORCHESTRA_INVENTORY_STOCK_OPNAME,
                null,
                userId
        );

        return ResponseEntity.ok("Inventory stock opname is successfully sent, please wait for the process to finished");
    }

    @Transactional
    public ResponseEntity<String> excelStockOpname(OutboxPublisherMessageDto outboxData) {
        DataBulkImportLog dataBulkImportLog =
            dataBulkImportLogService.getPendingDataBulkImportLog(outboxData.getEntityId());

        DataBulkImportLog dataBulkImportLogProxy =
            dataBulkImportLogService.getReferenceById(outboxData.getEntityId());

        List<InventoryStockOpnameExcelPayloadDto> listInventoryStockOpnameData = objectMapper.convertValue(
                outboxData.getPayload(),
                new TypeReference<List<InventoryStockOpnameExcelPayloadDto>>(){}
        );

        List<Inventory> listInventory = inventoryService.findAllByProductVariantIdInAndFreeze(
            listInventoryStockOpnameData.stream()
                .map(InventoryStockOpnameExcelPayloadDto::productVariantId)
                .collect(Collectors.toList()),
            true
        );

        List<DataBulkImportLogError> listDataBulkImportLogError = new ArrayList<>();
        List<InventoryStockOpnameDto> listInventoryStockOpname = new ArrayList<>();
        List<StockOpnameReview> listStockOpnameReview = new ArrayList<>();
        Set<UUID> listValidId = new HashSet<>();

        for (InventoryStockOpnameExcelPayloadDto currentInventoryStockOpname : listInventoryStockOpnameData) {
            Inventory currentInventory =
                listInventory.stream()
                    .filter(
                        inventory ->
                            inventory.getProductVariantId().equals(currentInventoryStockOpname.productVariantId())
                    )
                    .findFirst()
                    .orElse(null);

            if (currentInventory == null) System.out.println(currentInventoryStockOpname.productVariantId());

            if (currentInventory == null) {
                DataBulkImportLogErrorId dataBulkImportLogErrorId =
                    DataBulkImportLogErrorId.builder()
                        .rowNumber(currentInventoryStockOpname.rowNumber())
                        .build();

                DataBulkImportLogError dataBulkImportLogError =
                    DataBulkImportLogError.builder()
                        .dataBulkImportLogErrorId(dataBulkImportLogErrorId)
                        .dataBulkImportLog(dataBulkImportLogProxy)
                        .error("No inventory record found in DB")
                        .build();

                listDataBulkImportLogError.add(dataBulkImportLogError);
                continue;
            } else if (
                currentInventoryStockOpname.actualQuantity() - currentInventory.getQuantity() < 0
            ) {
                listStockOpnameReview.add(
                    StockOpnameReview.builder()
                        .sourceId(outboxData.getEntityId())
                        .systemQuantity(currentInventory.getQuantity())
                        .actualQuantity(currentInventoryStockOpname.actualQuantity())
                        .systemReservedQuantity(currentInventory.getReservedQuantity())
                        .productVariant(currentInventory.getProductVariant())
                        .build()
                );
                continue;
            }
            else if (
                currentInventoryStockOpname.actualQuantity() == currentInventory.getQuantity()
            ) {
               listValidId.add(currentInventory.getProductVariantId());
               continue;
            }

            listInventoryStockOpname.add(
                new InventoryStockOpnameDto(
                    currentInventory.getProductVariantId(),
                    currentInventory.getQuantity(),
                    currentInventoryStockOpname.actualQuantity(),
                    currentInventory.getReservedQuantity()
                )
            );
        }

        int[][] returnedStockOpname =
            inventoryService.inventoryStockOpname(listInventoryStockOpname, dataBulkImportLog.getUser().getId());

        for (int[] currentReturnedStockOpname : returnedStockOpname) {
            for (int i = 0; i < currentReturnedStockOpname.length; i++) {
                InventoryStockOpnameDto currentInventoryStockOpname =
                        listInventoryStockOpname.get(i);

                InventoryStockOpnameExcelPayloadDto currentInventoryStockOpnameData =
                    listInventoryStockOpnameData.stream()
                        .filter(
                            (stockOpnameData) ->
                                stockOpnameData.productVariantId().equals(currentInventoryStockOpname.productVariantId())
                        )
                        .findFirst()
                        .orElse(null);

                if (currentReturnedStockOpname[i] == 0) {
                    DataBulkImportLogErrorId dataBulkImportLogErrorId =
                        DataBulkImportLogErrorId.builder()
                            .rowNumber(currentInventoryStockOpnameData.rowNumber())
                            .build();

                    DataBulkImportLogError dataBulkImportLogError =
                        DataBulkImportLogError.builder()
                            .dataBulkImportLogErrorId(dataBulkImportLogErrorId)
                            .dataBulkImportLog(dataBulkImportLogProxy)
                            .error("Inventory failed to update")
                            .build();

                    listDataBulkImportLogError.add(dataBulkImportLogError);
                } else {
                    listValidId.add(currentInventoryStockOpnameData.productVariantId());
                }
            }
        }

        inventoryService.unfreezeInventories(listValidId);
        stockOpnameReviewRepository.saveAll(listStockOpnameReview);
        dataBulkImportLogService.insertBulkDataBulkLogError(listDataBulkImportLogError);
        return ResponseEntity.ok("Inventory stock opname finished");
    }

    @Transactional
    public ResponseEntity<String> rejectStockOpnameReview(
        RejectStockOpnameReviewDto rejectStockOpnameReviewData,
        UUID stockOpnameReviewId,
        UUID userId
    ) {
         StockOpnameReview stockOpnameReview =
            stockOpnameReviewRepository.findForUpdateById(stockOpnameReviewId)
                .orElseThrow(() -> new EntityNotFoundException("No Stock Opname Review found"));

        if (stockOpnameReview.getResolutionAction() != null)
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Stock opname review " + stockOpnameReviewId + " has already been resolved as "
                + stockOpnameReview.getResolutionAction()
                + " at " + stockOpnameReview.getReviewedAt()
            );

        UUID productVariantId = stockOpnameReview.getProductVariant().getId();

        stockOpnameReview.setRootCause(rejectStockOpnameReviewData.rootCause());
        stockOpnameReview.setInvestigationNote(rejectStockOpnameReviewData.investigationNote());
        stockOpnameReview.setResolutionAction(StockOpnameReviewResolutionEnum.REJECT);
        stockOpnameReview.setUser(userService.getReferenceById(userId));
        stockOpnameReview.setReviewedAt(LocalDateTime.now());

        stockOpnameReviewRepository.save(stockOpnameReview);

        inventoryService.unfreezeInventories(Set.of(productVariantId));

        return ResponseEntity.ok("Inventory stock opname review rejected successfully");
    }

    @Transactional
    public ResponseEntity<String> approveStockOpnameReview(
        ApproveStockOpnameReviewDto approveStockOpnameReviewData,
        UUID stockOpnameReviewId,
        UUID userId
    ) {
        StockOpnameReview stockOpnameReview =
            stockOpnameReviewRepository.findForUpdateById(stockOpnameReviewId)
                .orElseThrow(() -> new EntityNotFoundException("No Stock Opname Review found"));

        if (stockOpnameReview.getResolutionAction() != null)
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Stock opname review " + stockOpnameReviewId + " has already been resolved as "
                + stockOpnameReview.getResolutionAction()
                + " at " + stockOpnameReview.getReviewedAt()
            );

        UUID productVariantId = stockOpnameReview.getProductVariant().getId();

        Inventory inventory =
            inventoryService.findForUpdateByProductVariantIdAndFreeze(productVariantId, true)
                .orElseThrow(() -> new EntityNotFoundException("No Inventory found"));

        int shortage =
            Math.max(
                0,
                stockOpnameReview.getSystemReservedQuantity() - stockOpnameReview.getActualQuantity()
            );

        StockOpnameReviewResolutionEnum resolutionAction =
            approveStockOpnameReviewData.resolutionAction();

        validateStockOpnameResolutionAction(
            resolutionAction,
            shortage,
            productVariantId
        );

        List<InventoryLog> listInventoryLog = new ArrayList<>();

        int quantityChange =
            stockOpnameReview.getActualQuantity() - inventory.getQuantity();

        if (quantityChange != 0) {
            listInventoryLog.add(
                InventoryLog.builder()
                    .user(userService.getReferenceById(userId))
                    .quantityBefore(inventory.getQuantity())
                    .quantityAfter(stockOpnameReview.getActualQuantity())
                    .quantityChange(quantityChange)
                    .reservedQuantityBefore(inventory.getReservedQuantity())
                    .reservedQuantityAfter(inventory.getReservedQuantity())
                    .reservedQuantityChange(0)
                    .productVariant(
                        productService.getProductVariantReferenceById(productVariantId)
                    )
                    .referenceId(stockOpnameReviewId)
                    .type(InventoryLogTypeEnum.STOCK_OPNAME)
                    .build()
            );
        }

        int totalCancelledQuantity = 0;

        if (resolutionAction == StockOpnameReviewResolutionEnum.RESOLVE_RESERVATION_SHORTAGE) {
            List<UserBalanceMutationDto> listUserBalanceMutationData = new ArrayList<>();

            List<OrderDetailCancelResponseDto> listCancelOrderDetail =
                orderService.cancelOrderDetail(
                    productVariantId,
                    shortage,
                    userId
                );

            if (listCancelOrderDetail.isEmpty())
                throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Product variant " + productVariantId + " is short by " + shortage
                    + " reserved unit(s), but no cancellable reserved order detail was found. "
                    + "Refusing to approve stock opname review " + stockOpnameReviewId
                    + " because the shortage cannot be resolved."
                );

            for (OrderDetailCancelResponseDto currentOrderDetailCancel : listCancelOrderDetail) {
                listUserBalanceMutationData.add(
                    new UserBalanceMutationDto(
                        BigDecimal.ZERO,
                        currentOrderDetailCancel.cancelledSubTotal().negate(),
                        UserBalanceLogTypeEnum.REFUND_STOCK_SHORTAGE,
                        currentOrderDetailCancel.userId(),
                        null,
                        currentOrderDetailCancel.orderId()
                    )
                );
                totalCancelledQuantity += currentOrderDetailCancel.cancelledQuantity();
            }

            userBalanceService.refundMultipleUsers(listUserBalanceMutationData);

            if (totalCancelledQuantity > 0) {
                listInventoryLog.add(
                    InventoryLog.builder()
                        .user(userService.getReferenceById(userId))
                        .quantityBefore(inventory.getQuantity())
                        .quantityAfter(inventory.getQuantity())
                        .quantityChange(0)
                        .reservedQuantityBefore(inventory.getReservedQuantity())
                        .reservedQuantityAfter(
                            inventory.getReservedQuantity() - totalCancelledQuantity
                        )
                        .reservedQuantityChange(-totalCancelledQuantity)
                        .productVariant(
                            productService.getProductVariantReferenceById(productVariantId)
                        )
                        .referenceId(stockOpnameReviewId)
                        .type(InventoryLogTypeEnum.RESERVATION_RELEASE)
                        .build()
                );

                inventory.setReservedQuantity(
                    inventory.getReservedQuantity() - totalCancelledQuantity
                );
            }
        }

        if (stockOpnameReview.getActualQuantity() - inventory.getReservedQuantity() < 0)
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Cannot reconcile inventory for product variant " + productVariantId
                + " because the resulting available quantity would be negative"
                + " (actual_quantity=" + stockOpnameReview.getActualQuantity()
                + ", reserved_quantity_after=" + inventory.getReservedQuantity()
                + ")."
            );

        inventory.setQuantity(stockOpnameReview.getActualQuantity());
        inventoryService.save(inventory);

        if (!listInventoryLog.isEmpty())
            inventoryService.saveAllLog(listInventoryLog);

        stockOpnameReview.setRootCause(approveStockOpnameReviewData.rootCause());
        stockOpnameReview.setInvestigationNote(approveStockOpnameReviewData.investigationNote());
        stockOpnameReview.setResolutionAction(resolutionAction);
        stockOpnameReview.setUser(userService.getReferenceById(userId));
        stockOpnameReview.setReviewedAt(LocalDateTime.now());

        stockOpnameReviewRepository.save(stockOpnameReview);

        inventoryService.unfreezeInventories(Set.of(productVariantId));

        return ResponseEntity.ok("Inventory stock opname review approved successfully");
    }

    private void validateStockOpnameResolutionAction(
        StockOpnameReviewResolutionEnum resolutionAction,
        int shortage,
        UUID productVariantId
    ) {
        if (resolutionAction == null)
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "resolution_action is required. Allowed value: "
                + allowedResolutionActionMessage(shortage) + "."
            );

        if (resolutionAction == StockOpnameReviewResolutionEnum.REJECT)
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "resolution_action REJECT cannot be used on the approve endpoint. "
                + "Use the reject endpoint instead."
            );

        boolean resolvesShortage =
            resolutionAction == StockOpnameReviewResolutionEnum.RESOLVE_RESERVATION_SHORTAGE;

        if (shortage > 0 && !resolvesShortage)
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Product variant " + productVariantId + " is short by " + shortage
                + " reserved unit(s), so resolution_action must be RESOLVE_RESERVATION_SHORTAGE "
                + "instead of " + resolutionAction + "."
            );

        if (shortage == 0 && resolvesShortage)
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Product variant " + productVariantId + " has no reservation shortage, so "
                + "resolution_action RESOLVE_RESERVATION_SHORTAGE is not applicable. Allowed value: "
                + allowedResolutionActionMessage(0) + "."
            );
    }

    private String allowedResolutionActionMessage(int shortage) {
        if (shortage > 0)
            return StockOpnameReviewResolutionEnum.RESOLVE_RESERVATION_SHORTAGE.name();

        return StockOpnameReviewResolutionEnum.RECONCILE_TO_ACTUAL.name();
    }
}
