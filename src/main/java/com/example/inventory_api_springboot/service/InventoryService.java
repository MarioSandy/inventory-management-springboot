package com.example.inventory_api_springboot.service;

import com.example.inventory_api_springboot.dto.query.request.InventoryStockOpnameDto;
import com.example.inventory_api_springboot.dto.query.response.InventoryStockOpnameExcelDto;
import com.example.inventory_api_springboot.dto.request.InventoryDamageAdjustmentRequestDto;
import com.example.inventory_api_springboot.dto.request.InventoryRestockRequestDto;
import com.example.inventory_api_springboot.entity.Inventory;
import com.example.inventory_api_springboot.entity.InventoryLog;
import com.example.inventory_api_springboot.entity.ProductVariant;
import com.example.inventory_api_springboot.entity.User;
import com.example.inventory_api_springboot.enums.InventoryLogTypeEnum;
import com.example.inventory_api_springboot.enums.InventoryMutationResultEnum;
import com.example.inventory_api_springboot.repository.InventoryLogRepository;
import com.example.inventory_api_springboot.repository.inventory.InventoryRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InventoryService {
    private final InventoryRepository inventoryRepository;
    private final InventoryLogRepository inventoryLogRepository;
    private final UserService userService;
    private final UserBalanceService userBalanceService;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public Inventory getReferenceById(UUID id) {
        return inventoryRepository.getReferenceById(id);
    }

    @Transactional(readOnly = true)
    public InventoryLog getInventoryLogReferenceById(Long id) {
        return inventoryLogRepository.getReferenceById(id);
    }

    @Transactional
    public void mutation(
        UUID orderId,
        InventoryLogTypeEnum type,
        UUID userId
    ) {
        switch (type) {
            case ORDER_CANCELLED : {
                inventoryRepository.cancelOrder(orderId);
                inventoryLogRepository.insertReversedLog(
                    orderId,
                    InventoryLogTypeEnum.ORDER_CANCELLED,
                    userId
                );
                break;
            }
            case ORDER_SHIPPED: {
                inventoryRepository.finishOrder(orderId);
                inventoryLogRepository.insertReversedLog(
                    orderId,
                    InventoryLogTypeEnum.ORDER_SHIPPED,
                    userId
                );
                break;
            }
        }
    }

    private void mutationWithQuantityCheck(
        UUID id,
        int quantity,
        int reservedQuantity,
        InventoryLogTypeEnum type,
        UUID userId,
        UUID referenceId
    ) {
        InventoryMutationResultEnum mutationResult =
            mutation(
                id,
                quantity,
                reservedQuantity,
                type,
                userId,
                referenceId
            );

        switch (mutationResult) {
            case RESERVED_QUANTITY_EXCEEDS: {
                throw new IllegalArgumentException("Reserved quantity exceeds system inventory reserved quantity");
            }
            case QUANTITY_EXCEEDS: {
                throw new IllegalArgumentException(
                        "Quantity exceeds system inventory quantity or changed quantity below change reserved quantity"
                );
            }
        }
    }

    @Transactional
    public InventoryMutationResultEnum mutation(
        UUID id,
        int quantity,
        int reservedQuantity,
        InventoryLogTypeEnum type,
        UUID userId,
        UUID referenceId
    ) {
        type.validate(
            quantity,
            reservedQuantity
        );

        Inventory inventory = inventoryRepository.findForUpdateByProductVariantId(id)
            .orElseThrow(() -> new EntityNotFoundException("Inventory for product " + id + " is not found"));

        int quantityAfter = inventory.getQuantity() + quantity;
        int reservedQuantityAfter = inventory.getReservedQuantity() + reservedQuantity;

        if (reservedQuantityAfter < 0)
            return InventoryMutationResultEnum.RESERVED_QUANTITY_EXCEEDS;
        if (quantityAfter - reservedQuantityAfter < 0)
            return InventoryMutationResultEnum.QUANTITY_EXCEEDS;

        if (inventory.getFreeze() != null && inventory.getFreeze() == true) {
            return InventoryMutationResultEnum.FREEZED;
        } else {
            User userProxy = null;
            if (userId != null) userProxy = userService.getReferenceById(userId);

            inventory.setQuantity(quantityAfter);
            inventory.setReservedQuantity(reservedQuantityAfter);

            InventoryLog inventoryLog = InventoryLog.builder()
                .productVariant(entityManager.getReference(ProductVariant.class, id))
                .quantityBefore(inventory.getQuantity())
                .quantityAfter(quantityAfter)
                .quantityChange(quantity)
                .reservedQuantityBefore(inventory.getReservedQuantity())
                .reservedQuantityAfter(reservedQuantityAfter)
                .reservedQuantityChange(reservedQuantity)
                .type(type)
                .user(userProxy)
                .build();

            if (referenceId != null) inventoryLog.setReferenceId(referenceId);

            inventoryRepository.save(inventory);
            inventoryLogRepository.save(inventoryLog);
        }
        return InventoryMutationResultEnum.SUCCESS;
    }

    @Transactional
    public ResponseEntity<String> restock(
        UUID id,
        InventoryRestockRequestDto restockData,
        UUID userId
    ) {
        mutationWithQuantityCheck(
            id,
            restockData.quantity(),
            0,
            InventoryLogTypeEnum.RESTOCK,
            userId,
            null
        );

        return ResponseEntity.ok("Inventory restocked successfully");
    }

    @Transactional
    public ResponseEntity<String> damageAdjustment(
            UUID id,
            InventoryDamageAdjustmentRequestDto damageAdjustmentData,
            UUID userId
    ) {
        mutationWithQuantityCheck(
            id,
            damageAdjustmentData.quantity(),
            0,
            InventoryLogTypeEnum.DAMAGED,
            userId,
            null
        );

        return ResponseEntity.ok("Damaged inventory adjusted successfully");
    }

    @Transactional
    public List<InventoryStockOpnameExcelDto> getInventoryForStockOpnameExcel(UUID cursor) {
        return inventoryRepository.getInventoryForStockOpnameExcel(cursor);
    }

    @Transactional
    public void freezeInventories(Set<UUID> listProductVariantId) {
        inventoryRepository.freezeInventories(listProductVariantId);
    }

    @Transactional
    public void unfreezeInventories(Set<UUID> listValidId) {
        inventoryRepository.unfreezeInventories(listValidId);
    }

    @Transactional
    public List<Inventory> findAllByProductVariantIdInAndFreeze(List<UUID> listProductVariantIds, boolean isFreeze) {
        return inventoryRepository.findAllByProductVariantIdInAndFreeze(listProductVariantIds, isFreeze);
    }

    @Transactional
    public Optional<Inventory> findForUpdateByProductVariantIdAndFreeze(UUID productVariantId, boolean isFreeze) {
        return inventoryRepository.findForUpdateByProductVariantIdAndFreeze(productVariantId, isFreeze);
    }

    @Transactional
    public int[][] inventoryStockOpname(
        List<InventoryStockOpnameDto> listInventoryStockOpnameData,
        UUID userId
    ) {
        return
            inventoryRepository.inventoryStockOpname(
                listInventoryStockOpnameData,
                userId
            );
    }
}
