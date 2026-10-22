package com.example.inventory_api_springboot.service;

import com.example.inventory_api_springboot.dto.query.response.ProductBulkInsertReturningDto;
import com.example.inventory_api_springboot.dto.query.response.ProductInvalidMasterDataIdDto;
import com.example.inventory_api_springboot.dto.query.response.ProductTableDto;
import com.example.inventory_api_springboot.dto.query.response.ProductVariantPriceDto;
import com.example.inventory_api_springboot.dto.queue.OutboxPublisherMessageDto;
import com.example.inventory_api_springboot.dto.queue.ProductExcelCreatePayloadDto;
import com.example.inventory_api_springboot.dto.request.ProductCreateRequestDto;
import com.example.inventory_api_springboot.dto.request.ProductVariantCreateRequestDto;
import com.example.inventory_api_springboot.entity.Inventory;
import com.example.inventory_api_springboot.entity.Product;
import com.example.inventory_api_springboot.entity.DataBulkImportLog;
import com.example.inventory_api_springboot.entity.DataBulkImportLogError;
import com.example.inventory_api_springboot.entity.DataBulkImportLogErrorId;
import com.example.inventory_api_springboot.entity.ProductVariant;
import com.example.inventory_api_springboot.enums.OutboxEventTypeEnum;
import com.example.inventory_api_springboot.enums.DataBulkImportLogStatusEnum;
import com.example.inventory_api_springboot.projection.ProductBrandDropdownProjection;
import com.example.inventory_api_springboot.projection.ProductCategoryDropdownProjection;
import com.example.inventory_api_springboot.projection.ProductColorDropdownProjection;
import com.example.inventory_api_springboot.projection.ProductSizeDropdownProjection;
import com.example.inventory_api_springboot.repository.ProductVariantRepository;
import com.example.inventory_api_springboot.repository.product.ProductRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {
    // Repository
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;

    // Service
    private final ProductCategoryService productCategoryService;
    private final ProductBrandService productBrandService;
    private final ProductColorService productColorService;
    private final ProductSizeService productSizeService;
    private final DataBulkImportLogService dataBulkImportLogService;
    private final UserService userService;
    private final OutboxService outboxService;
    private final S3Service s3Service;

    private final ObjectMapper objectMapper;
    private final Validator validator;

    @Value("${spring.file-chunk.max-chunk-size.product-batch-create}")
    private int maxChunkSize;

    @Transactional(readOnly = true)
    public Product getReferenceById(UUID id) {
        return productRepository.getReferenceById(id);
    }

    @Transactional(readOnly = true)
    public ProductVariant getProductVariantReferenceById(UUID id) {
        return productVariantRepository.getReferenceById(id);
    }

    public List<ProductVariantPriceDto> findListIdAndPrice(Set<UUID> productVariantIds) {
        return productRepository.findListIdAndPrice(productVariantIds);
    }

    public ResponseEntity createProduct(ProductCreateRequestDto productCreateRequestData) {
        Product product =
            Product.builder()
                .name(productCreateRequestData.name())
                .description(productCreateRequestData.description())
                .productCategory(
                    productCategoryService.getReferenceById(productCreateRequestData.productCategoryId())
                )
                .productBrand(
                    productBrandService.getReferenceById(productCreateRequestData.productBrandId())
                )
                .basePrice(productCreateRequestData.basePrice())
                .build();

        List<ProductVariant> listProductVariants = new ArrayList<ProductVariant>();

        for (ProductVariantCreateRequestDto productVariantRequestData : productCreateRequestData.listDetail()) {
            ProductVariant productVariant =
                ProductVariant.builder()
                    .productColor(
                        productColorService.getReferenceById(productVariantRequestData.color())
                    )
                    .productSize(
                        productSizeService.getReferenceById(productVariantRequestData.size())
                    )
                    .price(productVariantRequestData.price())
                    .product(product)
                    .build();

            Inventory inventory =
                Inventory.builder()
                    .productVariant(productVariant)
                    .quantity(0)
                    .reservedQuantity(0)
                    .build();

            productVariant.setInventory(inventory);
            listProductVariants.add(productVariant);
        }

        product.setListProductVariants(listProductVariants);
        productRepository.save(product);

        return ResponseEntity.ok("Product created successfully");
    }

    public ResponseEntity<List<ProductTableDto>> getListProduct(
        String search,
        String brandId,
        String categoryId,
        String colorId,
        String size,
        String cursor,
        int limit
    ) {
        List<ProductTableDto> listProducts =
            productRepository.findTableData(
                search,
                brandId,
                categoryId,
                colorId,
                size,
                cursor,
                limit
            );

        return ResponseEntity.ok(listProducts);
    }

    private void writeValueToCell(Cell cell, Object value, Class<?> type) {
        System.out.println("Value : " + value);
        System.out.println("Type : " + type);
        if (value == null) {
            cell.setBlank(); // Handle null values safely
            return;
        }

        // Direct routing based on the registered type token
        if (type == String.class) {
            cell.setCellValue((String) value);
        } else if (type == Integer.class || type == int.class) {
            cell.setCellValue(((Integer) value).doubleValue()); // POI numbers must be doubles
        } else if (type == Double.class || type == double.class) {
            cell.setCellValue((Double) value);
        } else if (type == Boolean.class || type == boolean.class) {
            cell.setCellValue((Boolean) value);
        } else if (type == Date.class) {
            cell.setCellValue((Date) value);
        } else if (type == LocalDate.class) {
            cell.setCellValue((LocalDate) value);
        } else if (type == LocalDateTime.class) {
            cell.setCellValue((LocalDateTime) value);
        } else {
            // Fallback: Fall back to toString() for unknown custom object types
            cell.setCellValue(value.toString());
        }

    }

    private <T> void createHiddenDropdownExcelSheet(
        Workbook workbook,
        String sheetName,
        List<T> datas,
        List<String> headerNames,
        Map<Function<T, ?>, Class<?>> registry
    ) {
        System.out.println("masuk pasti");
        // Create sheet
        Sheet sheet = workbook.createSheet(sheetName);
        // Get header row
        Row row = sheet.createRow(0);
        // Extract field name
        for (int i = 0; i < headerNames.size(); i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(headerNames.get(i));
        }

        // Set the master datas
        for (int i = 0; i < datas.size(); i++) {
            T currentData = datas.get(i);
            Row currentRow = sheet.createRow(i + 1);

            AtomicInteger j = new AtomicInteger(0);
            registry.forEach((getterFunction, returnTypeClass) -> {
                Cell currentCell = currentRow.createCell(j.getAndIncrement());

                Object value = getterFunction.apply(currentData);
                writeValueToCell(currentCell, value, returnTypeClass);
            });
        }

        // 4. Secure the sheet
//        sheet.protectSheet(UUID.randomUUID().toString().replace("-", "").substring(0, 10));
//        int sheetIndex = workbook.getSheetIndex(sheetName);
//        workbook.setSheetVisibility(sheetIndex, SheetVisibility.VERY_HIDDEN);
    }

    private void addDropdownValidation(Sheet sheet, String formulaString, int startCol, int endCol, int lastRow) {
        DataValidationHelper validationHelper = sheet.getDataValidationHelper();
        CellRangeAddressList addressList = new CellRangeAddressList(1, lastRow, startCol, endCol);

        DataValidationConstraint constraint = validationHelper.createFormulaListConstraint(formulaString);
        DataValidation validation = validationHelper.createValidation(constraint, addressList);

        validation.setShowErrorBox(true);
        validation.setErrorStyle(DataValidation.ErrorStyle.STOP);

        sheet.addValidationData(validation);
    }

    public ResponseEntity<byte[]> createExcelTemplate() {
        List<ProductColorDropdownProjection> listProductColorDropdown = productColorService.getDropdown("");
        List<ProductSizeDropdownProjection> listProductSizeDropdown = productSizeService.getDropdown("");
        List<ProductCategoryDropdownProjection> listProductCategoryDropdown = productCategoryService.getDropdown("");
        List<ProductBrandDropdownProjection> listProductBrandDropdown = productBrandService.getDropdown("");

        try {
            Workbook workbook = new XSSFWorkbook();
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            // Product Color
            List<String> productColorSheetHeaderName = Arrays.asList("id", "name", "hexCode");
            Map<Function<ProductColorDropdownProjection, ?>, Class<?>> productColorDropdownRegistry = new LinkedHashMap<>();
            productColorDropdownRegistry.put(ProductColorDropdownProjection::getId, String.class);
            productColorDropdownRegistry.put(ProductColorDropdownProjection::getName, String.class);
            productColorDropdownRegistry.put(ProductColorDropdownProjection::getHexCode, String.class);

            createHiddenDropdownExcelSheet(
                    workbook,
                "product_color",
                    listProductColorDropdown,
                    productColorSheetHeaderName,
                    productColorDropdownRegistry
            );

            // Product Size
            List<String> productSizeSheetHeaderName = Arrays.asList("id", "name");
            Map<Function<ProductSizeDropdownProjection, ?>, Class<?>> productSizeDropdownRegistry = new LinkedHashMap<>();
            productSizeDropdownRegistry.put(ProductSizeDropdownProjection::getId, String.class);
            productSizeDropdownRegistry.put(ProductSizeDropdownProjection::getName, String.class);

            createHiddenDropdownExcelSheet(
                workbook,
                "product_size",
                listProductSizeDropdown,
                productSizeSheetHeaderName,
                productSizeDropdownRegistry
            );

            // Product Category
            List<String> productCategorySheetHeaderName = Arrays.asList("id", "name");
            Map<Function<ProductCategoryDropdownProjection, ?>, Class<?>> productCategoryDropdownRegistry = new LinkedHashMap<>();
            productCategoryDropdownRegistry.put(ProductCategoryDropdownProjection::getId, UUID.class);
            productCategoryDropdownRegistry.put(ProductCategoryDropdownProjection::getName, String.class);

            createHiddenDropdownExcelSheet(
                    workbook,
                "product_category",
                    listProductCategoryDropdown,
                    productCategorySheetHeaderName,
                    productCategoryDropdownRegistry
            );

            // Product Brand
            List<String> productBrandSheetHeaderName = Arrays.asList("id", "name", "code");
            Map<Function<ProductBrandDropdownProjection, ?>, Class<?>> productBrandDropdownRegistry = new LinkedHashMap<>();
            productBrandDropdownRegistry.put(ProductBrandDropdownProjection::getId, UUID.class);
            productBrandDropdownRegistry.put(ProductBrandDropdownProjection::getName, String.class);
            productBrandDropdownRegistry.put(ProductBrandDropdownProjection::getCode, String.class);

            createHiddenDropdownExcelSheet(
                workbook,
                "product_brand",
                listProductBrandDropdown,
                productBrandSheetHeaderName,
                productBrandDropdownRegistry
            );

            // Product
            Sheet productSheet = workbook.createSheet("product");
            Row productHeaderRow = productSheet.createRow(0);

            List<String> productSheetHeaderName = Arrays.asList(
                "name", "description", "category", "brand", "basePrice", "color", "size", "price"
            );

            for (int i = 0;  i < productSheetHeaderName.size(); i++) {
                Cell currentCell = productHeaderRow.createCell(i);
                currentCell.setCellValue(productSheetHeaderName.get(i));
            }

            addDropdownValidation(
                productSheet,
                "product_category!$B$2:$B$" + (listProductCategoryDropdown.size() + 1),
                    2,
                    2,
                    50000
            );

            addDropdownValidation(
                    productSheet,
                    "product_brand!$B$2:$B$" + (listProductBrandDropdown.size() + 1),
                    3,
                    3,
                    50000
            );

            addDropdownValidation(
                    productSheet,
                    "product_color!$B$2:$B$" + (listProductColorDropdown.size() + 1),
                    5,
                    5,
                    50000
            );

            addDropdownValidation(
                    productSheet,
                    "product_size!$B$2:$B$" + (listProductSizeDropdown.size() + 1),
                    6,
                    6,
                    50000
            );

            workbook.write(out);
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=product_template.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(out.toByteArray());
        }
        catch (Exception e) {
            throw new RuntimeException("Excel template generation failed: " + e.getMessage(), e);
        }
    }

    @Transactional
    public ResponseEntity<String> createBatchFromExcel(
        MultipartFile file,
        UUID userId
    ) throws IOException {
        // Store file to S3
        String key = s3Service.generateS3Key("products/create-batch-from-excel");
        s3Service.uploadFileToS3(key, file.getBytes());
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
            OutboxEventTypeEnum.FILE_ORCHESTRA_PRODUCT_BATCH_CREATE,
            null,
            userId
        );

        return ResponseEntity.ok("Product create batch is successfully sent, please wait for the process to finished");
    }

    private void addProductBatchLogError(
        int rowNumber,
        String errorMessage,
        List<DataBulkImportLogError> listDataBulkImportLogError,
        DataBulkImportLog dataBulkImportLogProxy
    ) {
        DataBulkImportLogErrorId dataBulkImportLogErrorId =
                DataBulkImportLogErrorId.builder()
                        .dataBulkImportLogId(dataBulkImportLogProxy.getId())
                        .rowNumber(rowNumber)
                        .build();

        DataBulkImportLogError dataBulkImportLogError =
                DataBulkImportLogError.builder()
                        .dataBulkImportLogErrorId(dataBulkImportLogErrorId)
                        .error(errorMessage)
                        .dataBulkImportLog(dataBulkImportLogProxy)
                        .build();

        listDataBulkImportLogError.add(dataBulkImportLogError);
    }

    private boolean isMasterDataExists(
        String sizeId,
        String colorId,
        UUID categoryId,
        UUID brandId,
        List<ProductInvalidMasterDataIdDto> listInvalidId,
        UUID productCreateBatchLogId,
        int rowNumber,
        List<DataBulkImportLogError> listDataBulkImportLogError,
        DataBulkImportLog dataBulkImportLogProxy
    ) {
        List<String> errorMessageBuilder = new ArrayList<>();

        for (ProductInvalidMasterDataIdDto currentInvalidId : listInvalidId) {
            switch(currentInvalidId.getEntityType()) {
                case "size": {
                    if (sizeId.equals(currentInvalidId.getId()))
                        errorMessageBuilder.add("Size id is not exists in DB");
                    break;
                }
                case "color": {
                    if (colorId.equals(currentInvalidId.getId()))
                        errorMessageBuilder.add("Color id is not exists in DB");
                    break;
                }
                case "category": {
                    if (
                        categoryId.equals(
                            UUID.fromString(currentInvalidId.getId())
                        )
                    )
                        errorMessageBuilder.add("Category id is not exists in DB");
                    break;
                }
                case "brand": {
                    if (
                        brandId.equals(
                            UUID.fromString(currentInvalidId.getId())
                        )
                    )
                        errorMessageBuilder.add("Brand id is not exists in DB");
                    break;
                }
            }

        }

        boolean isExists = errorMessageBuilder.isEmpty();
        if (!isExists) {
            addProductBatchLogError(
                    rowNumber,
                    String.join(", ", errorMessageBuilder),
                    listDataBulkImportLogError,
                    dataBulkImportLogProxy
            );
        }

        return isExists;
    }

    private boolean isProductExcelRowValid(
        UUID productCreateBatchLogId,
        ProductCreateRequestDto productCreateData,
        int rowNumber,
        List<DataBulkImportLogError> listDataBulkImportLogError,
        DataBulkImportLog dataBulkImportLogProxy
    ) {
        Set<ConstraintViolation<ProductCreateRequestDto>> violations =
                validator.validate(productCreateData);

        boolean isValid = violations.size() == 0;

        if (!isValid) {
            String errorMessage = violations.stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .collect(Collectors.joining(", "));

            addProductBatchLogError(
                    rowNumber,
                    String.join(errorMessage),
                    listDataBulkImportLogError,
                    dataBulkImportLogProxy
            );
        }

        return isValid;
    }

    @Transactional()
    public void insertProductBatch(
        OutboxPublisherMessageDto outboxData
    ) {
        DataBulkImportLog dataBulkImportLog =
            dataBulkImportLogService.getPendingDataBulkImportLog(outboxData.getEntityId());

        DataBulkImportLog dataBulkImportLogProxy =
            dataBulkImportLogService.getReferenceById(outboxData.getEntityId());

        List<ProductExcelCreatePayloadDto> listProductCreateData = objectMapper.convertValue(
            outboxData.getPayload(),
            new TypeReference<List<ProductExcelCreatePayloadDto>>(){}
        );

        Set<String> listProductColorId = new HashSet<>();
        Set<String> listProductSizeId = new HashSet<>();
        Set<UUID> listProductCategoryId = new HashSet<>();
        Set<UUID> listProductBrandId = new HashSet<>();

        for (ProductExcelCreatePayloadDto currentExcelProductData : listProductCreateData) {
            ProductCreateRequestDto currentProductData = currentExcelProductData.productCreateData();

            listProductColorId.add(currentProductData.listDetail().get(0).color());
            listProductSizeId.add(currentProductData.listDetail().get(0).size());
            listProductCategoryId.add(currentProductData.productCategoryId());
            listProductBrandId.add(currentProductData.productBrandId());
        }

        // Fetch master datas
        List<ProductInvalidMasterDataIdDto> listInvalidMasterDataId =
            productRepository.checkInvalidMasterDataIds(
                listProductSizeId,
                listProductColorId,
                listProductCategoryId,
                listProductBrandId
            );

        List<String> listInsertProductName = new ArrayList<>();
        List<String> listInsertProductDescription = new ArrayList<>();
        List<UUID> listInsertProductCategoryId = new ArrayList<>();
        List<UUID> listInsertProductBrandId = new ArrayList<>();
        List<BigDecimal> listInsertProductBasePrice = new ArrayList<>();
        List<DataBulkImportLogError> listDataBulkImportLogError = new ArrayList<>();

        for (ProductExcelCreatePayloadDto currentProductExcelData : listProductCreateData) {
            ProductCreateRequestDto currentProductData = currentProductExcelData.productCreateData();
            ProductVariantCreateRequestDto currentProductVariant = currentProductData.listDetail().get(0);

            if (
                !isProductExcelRowValid(
                    dataBulkImportLog.getId(),
                    currentProductData,
                    currentProductExcelData.rowNumber(),
                        listDataBulkImportLogError,
                        dataBulkImportLogProxy
                )
            ) continue;

            if (
                !listInvalidMasterDataId.isEmpty()
                && !isMasterDataExists(
                    currentProductVariant.size(),
                    currentProductVariant.color(),
                    currentProductData.productCategoryId(),
                    currentProductData.productBrandId(),
                    listInvalidMasterDataId,
                    dataBulkImportLog.getId(),
                    currentProductExcelData.rowNumber(),
                        listDataBulkImportLogError,
                        dataBulkImportLogProxy
                )
            ) continue;

            Product product = Product.builder()
                .name(currentProductData.name())
                .description(currentProductData.description())
                .productCategory(
                    productCategoryService.getReferenceById(
                        currentProductData.productCategoryId()
                    )
                )
                .productBrand(
                    productBrandService.getReferenceById(
                        currentProductData.productBrandId()
                    )
                )
                .basePrice(currentProductData.basePrice())
                .build();

            List<ProductVariant> listVariant = Arrays.asList(
                ProductVariant.builder()
                    .product(product)
                    .productSize(
                        productSizeService.getReferenceById(
                            currentProductVariant.size()
                        )
                    )
                    .productColor(
                        productColorService.getReferenceById(
                            currentProductVariant.color()
                        )
                    )
                    .price(currentProductVariant.price())
                    .build()
            );

            product.setListProductVariants(listVariant);
            listInsertProductName.add(currentProductData.name());
            listInsertProductDescription.add(currentProductData.description());
            listInsertProductCategoryId.add(currentProductData.productCategoryId());
            listInsertProductBrandId.add(currentProductData.productBrandId());
            listInsertProductBasePrice.add(currentProductData.basePrice());
        }

        System.out.println(
            "BEFORE INSERT tx active=" +
            TransactionSynchronizationManager.isActualTransactionActive() +
            ", name=" +
            TransactionSynchronizationManager.getCurrentTransactionName()
        );

        List<ProductBulkInsertReturningDto> listReturnedProduct =
            productRepository.insertBulkProducts(
                listInsertProductName.toArray(new String[0]),
                listInsertProductDescription.toArray(new String[0]),
                listInsertProductCategoryId.toArray(new UUID[0]),
                listInsertProductBrandId.toArray(new UUID[0]),
                listInsertProductBasePrice.toArray(new BigDecimal[0])
            );

        List<ProductVariant> listProductVariant = new ArrayList<>();
        boolean isProductFound = false;

        for (ProductExcelCreatePayloadDto currentProductExcelData : listProductCreateData) {
            ProductCreateRequestDto currentProductData = currentProductExcelData.productCreateData();

            for (ProductBulkInsertReturningDto returnedProduct : listReturnedProduct) {
                if (
                    currentProductData.name().equals(returnedProduct.getName())
                    && currentProductData.productBrandId().equals(returnedProduct.getProductBrandId())
                ) {
                    ProductVariantCreateRequestDto currentProductVariantCreateData = currentProductData.listDetail().get(0);
                    ProductVariant productVariant =
                        ProductVariant.builder()
                            .productSize(
                                productSizeService.getReferenceById(
                                        currentProductVariantCreateData.size()
                                )
                            )
                            .productColor(
                                productColorService.getReferenceById(
                                        currentProductVariantCreateData.color()
                                )
                            )
                            .price(currentProductVariantCreateData.price())
                            .product(
                                productRepository.getReferenceById(returnedProduct.getId())
                            )
                            .build();
                    productVariant.generateSku();

                    listProductVariant.add(productVariant);

                    isProductFound = true;
                    break;
                }
            }

            if (!isProductFound) {
                addProductBatchLogError(
                        currentProductExcelData.rowNumber(),
                        "No inserted product data id is found for name : "
                                + currentProductData.name()
                                + " & brand : "
                                + currentProductData.productBrandId(),
                        listDataBulkImportLogError,
                        dataBulkImportLogProxy
                );
            }

            isProductFound = false;
        }

        int[][] resultVariants = productRepository.insertBulkProductVariants(listProductVariant);

        for (int[] currentResultVariant : resultVariants) {
            for (int i = 0; i < currentResultVariant.length; i++) {
                if (currentResultVariant[i] == 0) {
                    addProductBatchLogError(
                        listProductCreateData.get(i).rowNumber(),
                        "Product variant has duplicates in DB",
                        listDataBulkImportLogError,
                        dataBulkImportLogProxy
                    );
                }
            }
        }

        if (!listDataBulkImportLogError.isEmpty()) {
            dataBulkImportLogService.insertBulkDataBulkLogError(listDataBulkImportLogError);
        }

        dataBulkImportLogService.incrementProcessedChunk(outboxData.getEntityId());
        dataBulkImportLogService.updateStatusFinish(outboxData.getEntityId());
    }
}
