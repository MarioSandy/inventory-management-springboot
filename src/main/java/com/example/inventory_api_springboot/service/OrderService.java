package com.example.inventory_api_springboot.service;

import com.example.inventory_api_springboot.dto.query.request.UserBalanceMutationDto;
import com.example.inventory_api_springboot.dto.query.response.OrderReadyProcessDetailDto;
import com.example.inventory_api_springboot.dto.query.response.OrderReadyProcessDto;
import com.example.inventory_api_springboot.dto.query.response.ProductVariantPriceDto;
import com.example.inventory_api_springboot.dto.queue.OutboxPublisherMessageDto;
import com.example.inventory_api_springboot.dto.request.OrderCreate.OrderCreateDetailRequestDto;
import com.example.inventory_api_springboot.dto.request.OrderCreate.OrderCreateRequestDto;
import com.example.inventory_api_springboot.dto.response.OrderDetailCancelResponseDto;
import com.example.inventory_api_springboot.entity.Inventory;
import com.example.inventory_api_springboot.entity.InventoryLog;
import com.example.inventory_api_springboot.entity.Order;
import com.example.inventory_api_springboot.entity.OrderDetail;
import com.example.inventory_api_springboot.entity.ProductVariant;
import com.example.inventory_api_springboot.enums.InventoryLogTypeEnum;
import com.example.inventory_api_springboot.enums.InventoryMutationResultEnum;
import com.example.inventory_api_springboot.enums.OrderStatusEnum;
import com.example.inventory_api_springboot.enums.OutboxEventTypeEnum;
import com.example.inventory_api_springboot.enums.UserBalanceLogTypeEnum;
import com.example.inventory_api_springboot.repository.order.OrderRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {
    private static final String RESERVATION_APPLY_FAILED_MESSAGE =
        "Unexpected inventory mutation result during reservation apply: %s for product variant %s of order %s. "
        + "Throwing so the transaction rolls back instead of committing a partial reservation leak.";

    private final OrderRepository orderRepository;
    private final InventoryService inventoryService;
    private final ProductService productService;
    private final UserService userService;
    private final UserBalanceService userBalanceService;
    private final OutboxService outboxService;
    private final TransactionTemplate transactionTemplate;

    @PersistenceContext
    private EntityManager entityManager; // Inject the standard JPA entity manager

    @Transactional(readOnly = true)
    public Order getReferenceById(UUID id) {
        return orderRepository.getReferenceById(id);
    }

    @Transactional
    public ResponseEntity<String> create(
        OrderCreateRequestDto orderData,
        UUID userId
    ) {
        // Get Product Variant Datas
        Set<UUID> productVariantIds = orderData.listDetail().stream()
            .map(OrderCreateDetailRequestDto::productVariantId)
            .collect(Collectors.toCollection(HashSet::new));

        List<ProductVariantPriceDto> listProductVariantPrice = productService.findListIdAndPrice(productVariantIds);

        // Create Order
        Order order = Order.builder()
            .status(OrderStatusEnum.PENDING)
            .user(userService.getReferenceById(userId))
            .build();

        BigDecimal totalPrice = BigDecimal.ZERO;
        List<OrderDetail> listOrderDetail = new ArrayList<OrderDetail>();

        for (OrderCreateDetailRequestDto orderDetailData : orderData.listDetail()) {
            // Calculate subtotal
            ProductVariantPriceDto currentProductPrice = listProductVariantPrice.stream()
                .filter(
                    productPrice -> productPrice.id().equals(orderDetailData.productVariantId())
                )
                .findFirst()
                .orElseThrow(
                    () -> new EntityNotFoundException(
                        "For product variant : " + orderDetailData.productVariantId() + " not found !"
                    )
                );

            if (currentProductPrice.price().compareTo(orderDetailData.price()) != 0)
                throw new ResponseStatusException(HttpStatus.CONFLICT, "The price of this item has changed. Please try again.");

            BigDecimal currentSubTotal = currentProductPrice.price().multiply(BigDecimal.valueOf(orderDetailData.quantity()));

            OrderDetail orderDetail = OrderDetail.builder()
                .order(order)
                .productVariant(entityManager.getReference(ProductVariant.class, orderDetailData.productVariantId()))
                .quantity(orderDetailData.quantity())
                .unitPrice(currentProductPrice.price())
                .subTotal(currentSubTotal)
                .build();

            listOrderDetail.add(orderDetail);
            totalPrice = totalPrice.add(currentSubTotal);
        }

        order.setListOrderDetails(listOrderDetail);
        Order savedOrder = orderRepository.save(order);

        // Reserve Balance
        userBalanceService.mutation(
            new UserBalanceMutationDto(
                BigDecimal.ZERO,
                totalPrice,
                UserBalanceLogTypeEnum.ORDER_RESERVED,
                userId,
                null,
                savedOrder.getId()
            )
        );

        return ResponseEntity.ok("Order successfully created");
    }

    @Transactional
    public ResponseEntity<String> cancel(UUID id) {
        Order order = orderRepository.findByIdAndStatus(id, OrderStatusEnum.PENDING)
                .orElseThrow(() -> new EntityNotFoundException("Order not found"));

        order.setStatus(OrderStatusEnum.CANCELLED);

        userBalanceService.mutation(
            id,
            UserBalanceLogTypeEnum.ORDER_CANCELLED
        );

//        inventoryService.mutation(
//            id,
//            InventoryLogTypeEnum.ORDER_CANCELLED,
//            userId
//        );

        return ResponseEntity.ok("Order cancelled successfully");
    }

    @Transactional
    public ResponseEntity<String> finish(
        UUID id,
        UUID userId
    ) {
        Order order = orderRepository.findWithDetailsByIdAndStatus(id, OrderStatusEnum.RESERVED)
            .orElseThrow(() -> new EntityNotFoundException("Order not found"));

        order.setStatus(OrderStatusEnum.COMPLETED);

        List<Inventory> listInventory =
            inventoryService.findAllForUpdateByProductVariantIdInAndFreeze(
                order.getListOrderDetails().stream()
                    .map(OrderDetail::getProductVariant)
                    .filter(Objects::nonNull)
                    .map(ProductVariant::getId)
                    .collect(Collectors.toList()),
                false
            );
        List<InventoryLog> listInventoryLog = new ArrayList<>();

        userBalanceService.mutation(
            id,
            UserBalanceLogTypeEnum.ORDER_SHIPPED
        );

        for (OrderDetail orderDetail : order.getListOrderDetails()) {
//            inventoryService.mutation(
//                orderDetail.getId(),
//                InventoryLogTypeEnum.ORDER_SHIPPED,
//                userId
//            );
            Inventory inventory =
                listInventory.stream()
                    .filter(
                        (currentInventory) ->
                            currentInventory.getProductVariantId().equals(orderDetail.getProductVariant().getId())
                    )
                    .findFirst()
                    .orElseThrow(() -> new EntityNotFoundException("No inventory found"));

            int quantityAfter = inventory.getQuantity() - orderDetail.getQuantity();
            int reservedQuantityAfter = inventory.getReservedQuantity() - orderDetail.getQuantity();
            int quantityChange = inventory.getQuantity() * -1;

            InventoryLog inventoryLog =
                InventoryLog.builder()
                    .quantityBefore(inventory.getQuantity())
                    .quantityAfter(quantityAfter)
                    .quantityChange(quantityChange)
                    .reservedQuantityBefore(inventory.getReservedQuantity())
                    .reservedQuantityAfter(reservedQuantityAfter)
                    .reservedQuantityChange(quantityChange)
                    .type(InventoryLogTypeEnum.ORDER_SHIPPED)
                    .build();

            listInventoryLog.add(inventoryLog);

            inventory.setQuantity(inventory.getQuantity() - orderDetail.getQuantity());
            inventory.setReservedQuantity(inventory.getReservedQuantity() - orderDetail.getQuantity());
        }

        inventoryService.saveAll(listInventory);
        inventoryService.saveAllLog(listInventoryLog);
        orderRepository.save(order);

        return ResponseEntity.ok("Order finished successfully");
    }

    @Transactional
    public List<OrderDetailCancelResponseDto> cancelOrderDetail(UUID productVariantId, int shortage, UUID userId) {
        return orderRepository.cancelOrderDetail(productVariantId, shortage);
    }

    private void pushChunkToOutbox(
        List<UUID> chunked
    ) {
        for (UUID readyOrder : chunked) {
            outboxService.create(
                readyOrder,
                "ORDER",
                OutboxEventTypeEnum.ORDER_PROCESSING,
                null,
                null
            );
        }

        chunked.clear();
    }

    @Transactional
    public void checkReadyOrderAndPushToOutbox() {
        List<UUID> listReadyOrder = orderRepository.getReadyPendingOrderIdsForProcess();
        List<UUID> chunked = new ArrayList<>();

        for (UUID readyOrder : listReadyOrder) {
            chunked.add(readyOrder);

             if (chunked.size() == 100) {
                transactionTemplate.executeWithoutResult(
                    result -> {
                        pushChunkToOutbox(chunked);
                    }
                );
            }
        }

        if (!chunked.isEmpty()) {
            transactionTemplate.executeWithoutResult(
                result -> {
                    pushChunkToOutbox(chunked);
                }
            );
        }
    }

    /**
     * Memproses order berstatus PROCESSING: reserve inventory untuk setiap detail, lalu
     * transisikan order sesuai hasilnya.
     *
     * <p>Isu CRIT-08: reservasi yang sudah sukses pada detail 1..N-1 tetap ter-commit ketika
     * detail ke-N gagal, karena return normal dari {@code @Transactional} bukan rollback.
     * Perbaikan di sini memakai pola <b>validate-first</b>: seluruh detail divalidasi lebih
     * dulu (read-only, di bawah lock {@code FOR UPDATE}), dan mutasi baru diterapkan setelah
     * semua detail lolos. Dengan begitu, jalur gagal benar-benar tidak meninggalkan jejak
     * (tidak ada perubahan inventory, tidak ada {@code inventory_log}).
     *
     * <ul>
     *   <li><b>Fase 1 (validate)</b>: lock semua inventory target yang tidak frozen,
     *       lalu validasi agregat per-variant ({@code reserved < 0}, {@code available < 0}).
     *       Tidak ada mutasi di fase ini.</li>
     *   <li><b>FREEZED</b>: order kembali ke {@code PENDING}. Scheduler
     *       ({@code checkReadyOrderAndPushToOutbox}) akan me-re-enqueue setelah unfreeze,
     *       jadi tidak memakai exception (outbox hanya retry 3x).</li>
     *   <li><b>QUANTITY_EXCEEDS / RESERVED_QUANTITY_EXCEEDS</b>: refund penuh
     *       {@code reserved_balance} order (bukan parsial, karena order di-CANCELLED
     *       seluruhnya), lalu transisi ke {@code CANCELLED}. Tidak ada mutasi inventory.</li>
     *   <li><b>Fase 2 (apply)</b>: semua detail sudah tervalidasi, panggil
     *       {@code InventoryService.mutation(ORDER_RESERVED)} per detail. Kegagalan tak
     *       terduga di fase ini melempar exception agar transaksi rollback total.</li>
     * </ul>
     */
    @Transactional
    public void processOrder(OutboxPublisherMessageDto orderData) {
        OrderReadyProcessDto orderReadyProcess =
            orderRepository.getReadyProcessOrder(orderData.getEntityId())
                .orElseThrow(() -> new EntityNotFoundException("Order not found"));

        List<OrderReadyProcessDetailDto> listOrderDetail = orderReadyProcess.getListDetail();

        if (listOrderDetail.isEmpty()) {
            orderRepository.updateOrderStatus(
                orderReadyProcess.getId(),
                OrderStatusEnum.RESERVED
            );
            return;
        }

        Set<UUID> variantIds = listOrderDetail.stream()
            .map(OrderReadyProcessDetailDto::productVariantId)
            .collect(Collectors.toCollection(LinkedHashSet::new));

        // Fase 1: lock semua inventory target yang tidak frozen, lalu validasi.
        // Tidak ada mutasi di sini, sehingga jalur gagal di bawah tidak meninggalkan
        // perubahan apa pun.
        List<Inventory> lockedInventories =
            inventoryService.findAllForUpdateByProductVariantIdInAndFreeze(variantIds, false);
        Map<UUID, Inventory> inventoryByVariantId = lockedInventories.stream()
            .collect(Collectors.toMap(Inventory::getProductVariantId, Function.identity()));

        InventoryMutationResultEnum failureResult = null;
        Map<UUID, Integer> simulatedReservedDelta = new HashMap<>();

        for (OrderReadyProcessDetailDto detail : listOrderDetail) {
            Inventory inventory = inventoryByVariantId.get(detail.productVariantId());

            if (inventory == null) {
                // Tidak masuk hasil lock: inventory frozen (difilter query) atau tidak ada.
                // Diperlakukan sebagai TRANSIENT agar scheduler retry setelah unfreeze.
                failureResult = InventoryMutationResultEnum.FREEZED;
                break;
            }

            int currentReserved = inventory.getReservedQuantity()
                + simulatedReservedDelta.getOrDefault(detail.productVariantId(), 0);
            int reservedAfter = currentReserved + detail.quantity();
            int availableAfter = inventory.getQuantity() - reservedAfter;

            if (reservedAfter < 0) {
                failureResult = InventoryMutationResultEnum.RESERVED_QUANTITY_EXCEEDS;
                break;
            }
            if (availableAfter < 0) {
                failureResult = InventoryMutationResultEnum.QUANTITY_EXCEEDS;
                break;
            }

            simulatedReservedDelta.merge(
                detail.productVariantId(),
                detail.quantity(),
                Integer::sum
            );
        }

        if (failureResult != null) {
            // Belum ada mutasi apa pun di fase 1. Tangani kegagalan tanpa meninggalkan
            // perubahan inventory maupun inventory_log.
            if (failureResult == InventoryMutationResultEnum.FREEZED) {
                // Transient: scheduler akan re-enqueue setelah inventory tidak frozen.
                orderRepository.updateOrderStatus(
                    orderReadyProcess.getId(),
                    OrderStatusEnum.PENDING
                );
            } else {
                // Shortage permanen: refund penuh reserved_balance lalu CANCELLED.
                BigDecimal orderTotal = listOrderDetail.stream()
                    .map(OrderReadyProcessDetailDto::subTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

                userBalanceService.refund(
                    new UserBalanceMutationDto(
                        BigDecimal.ZERO,
                        orderTotal.multiply(BigDecimal.valueOf(-1)),
                        UserBalanceLogTypeEnum.REFUND_STOCK_SHORTAGE,
                        orderReadyProcess.getUserId(),
                        null,
                        orderReadyProcess.getId()
                    )
                );

                orderRepository.updateOrderStatus(
                    orderReadyProcess.getId(),
                    OrderStatusEnum.CANCELLED
                );
            }
            return;
        }

        // Fase 2: semua detail sudah tervalidasi. Terapkan reservasi. Kegagalan tak
        // terduga (mis. freeze terjadi setelah validasi) melempar exception sehingga
        // transaksi rollback total (tidak ada reservasi yatim yang tertinggal).
        for (OrderReadyProcessDetailDto detail : listOrderDetail) {
            InventoryMutationResultEnum mutationResult =
                inventoryService.mutation(
                    detail.productVariantId(),
                    0,
                    detail.quantity(),
                    InventoryLogTypeEnum.ORDER_RESERVED,
                    null,
                    detail.orderDetailId()
                );

            if (mutationResult != InventoryMutationResultEnum.SUCCESS) {
                throw new IllegalStateException(
                    String.format(
                        RESERVATION_APPLY_FAILED_MESSAGE,
                        mutationResult,
                        detail.productVariantId(),
                        orderReadyProcess.getId()
                    )
                );
            }
        }

        orderRepository.updateOrderStatus(
            orderReadyProcess.getId(),
            OrderStatusEnum.RESERVED
        );
    }
}
