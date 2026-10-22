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
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {
    private static final String RELEASE_RESERVATION_FAILED_MESSAGE =
        "Failed to release reservation for product variant %s (result: %s) while cancelling order %s. "
        + "Throwing so the transaction rolls back instead of committing a partial release that would "
        + "silently leak inventory.";

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
     * <p>Isu CRIT-08: iterasi sebelumnya meninggalkan reservasi inventory yatim ketika
     * detail ke-N gagal, karena blok error hanya melakukan refund balance lalu return
     * (return normal = commit, tidak ada rollback). Perbaikan di sini:
     *
     * <ul>
     *   <li>Detail yang sukses direserve dicatat di {@code listReservedDetail}.</li>
     *   <li>Jika ada kegagalan, seluruh reservasi yang sudah sukses di-release melalui
     *       {@link InventoryLogTypeEnum#RESERVATION_RELEASE} sehingga
     *       {@code inventory.reserved_quantity} kembali ke nilai sebelum transaksi dan
     *       {@code inventory_log} tetap punya jejak audit.</li>
     *   <li>{@code FREEZED}: order kembali ke {@code PENDING}. Scheduler
     *       ({@code getReadyPendingOrderIdsForProcess}) akan me-skip order yang masih
     *       menyentuh inventory frozen, lalu memprosesnya kembali setelah unfreeze.
     *       Tidak melempar exception karena outbox hanya retry 3x (lihat
     *       {@code OutboxRepository.getAndMarkedPendingOutbox}); kalau freeze lebih lama
     *       dari itu order akan nyangkut di PROCESSING.</li>
     *   <li>{@code QUANTITY_EXCEEDS} / {@code RESERVED_QUANTITY_EXCEEDS}: refund penuh
     *       {@code reserved_balance} order (bukan parsial) karena order di-CANCELLED
     *       seluruhnya, lalu transisi ke {@code CANCELLED}.</li>
     *   <li>Kegagalan saat release (mis. variant ke-freeze di sela reserve dan release)
     *       melempar {@link IllegalStateException} agar transaksi rollback total —
     *       mencegah state "sebagian directed, sebagian masih ter-reserve".</li>
     * </ul>
     */
    @Transactional
    public void processOrder(OutboxPublisherMessageDto orderData) {
        OrderReadyProcessDto orderReadyProcess =
            orderRepository.getReadyProcessOrder(orderData.getEntityId())
                .orElseThrow(() -> new EntityNotFoundException("Order not found"));

        List<OrderReadyProcessDetailDto> listOrderDetail = orderReadyProcess.getListDetail();

        // Refund yang benar adalah total order penuh (semua detail), bukan prefix sampai
        // detail yang gagal. Saldo user di-reserve penuh saat create(), jadi saat order
        // dibatalkan seluruh reservasi balance harus dilepas.
        BigDecimal orderTotal = listOrderDetail.stream()
            .map(OrderReadyProcessDetailDto::subTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<OrderReadyProcessDetailDto> listReservedDetail = new ArrayList<>();
        InventoryMutationResultEnum failureResult = null;

        for (OrderReadyProcessDetailDto orderReadyProcessDetail : listOrderDetail) {
            // Reserve Stock
            InventoryMutationResultEnum mutationResult =
                inventoryService.mutation(
                    orderReadyProcessDetail.productVariantId(),
                    0,
                    orderReadyProcessDetail.quantity(),
                    InventoryLogTypeEnum.ORDER_RESERVED,
                    null,
                    orderReadyProcessDetail.orderDetailId()
                );

            if (mutationResult == InventoryMutationResultEnum.SUCCESS) {
                listReservedDetail.add(orderReadyProcessDetail);
                continue;
            }

            failureResult = mutationResult;
            break;
        }

        if (failureResult != null) {
            // Release semua reservasi yang sudah sukses sebelum kegagalan. Release yang
            // gagal melempar exception sehingga transaksi rollback total (tidak ada
            // reservasi yatim yang tertinggal).
            releaseReservations(listReservedDetail, orderReadyProcess.getId());

            if (failureResult == InventoryMutationResultEnum.FREEZED) {
                // Kembalikan ke PENDING: scheduler akan memproses ulang setelah unfreeze.
                // Semua reservasi yang sempat sukses sudah di-release di atas, jadi retry
                // berikutnya tidak menggandakan reserved_quantity.
                orderRepository.updateOrderStatus(
                    orderReadyProcess.getId(),
                    OrderStatusEnum.PENDING
                );
            } else {
                // Shortage: refund penuh + CANCELLED.
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

        orderRepository.updateOrderStatus(
            orderReadyProcess.getId(),
            OrderStatusEnum.RESERVED
        );
    }

    /**
     * Release reservasi inventory untuk setiap detail yang sukses sebelum kegagalan,
     * menggunakan {@link InventoryLogTypeEnum#RESERVATION_RELEASE} (arah negatif).
     *
     * <p>Urutan release mengikuti urutan detail, jadi lock pesimis yang sudah diambil
     * pada fase reserve tetap dipegang (tidak ada akuisisi lock baru → tidak ada risiko
     * deadlock antar detail dalam transaksi yang sama).
     */
    private void releaseReservations(
        List<OrderReadyProcessDetailDto> listReservedDetail,
        UUID orderId
    ) {
        for (OrderReadyProcessDetailDto reservedDetail : listReservedDetail) {
            InventoryMutationResultEnum releaseResult =
                inventoryService.mutation(
                    reservedDetail.productVariantId(),
                    0,
                    -reservedDetail.quantity(),
                    InventoryLogTypeEnum.RESERVATION_RELEASE,
                    null,
                    reservedDetail.orderDetailId()
                );

            if (releaseResult != InventoryMutationResultEnum.SUCCESS) {
                throw new IllegalStateException(
                    String.format(
                        RELEASE_RESERVATION_FAILED_MESSAGE,
                        reservedDetail.productVariantId(),
                        releaseResult,
                        orderId
                    )
                );
            }
        }
    }
}
