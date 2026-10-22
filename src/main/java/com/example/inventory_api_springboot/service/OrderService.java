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
    private static final String ORDER_STATUS_TRANSITION_FAILED_MESSAGE =
        "Order status transition %s -> %s affected %d row(s) instead of 1 for order id %s. " +
        "The expected status guard was likely not satisfied (order was already moved by " +
        "another transaction), so the transition was rejected.";

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
        Order order = orderRepository.findByIdAndStatus(id, OrderStatusEnum.RESERVED)
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
        List<UUID> readyOrderIds = orderRepository.getReadyPendingOrderIdsForProcess();
        List<UUID> chunked = new ArrayList<>();

        for (UUID readyOrder : readyOrderIds) {
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

    @Transactional
    public void processOrder(OutboxPublisherMessageDto orderData) {
        OrderReadyProcessDto orderReadyProcess =
            orderRepository.getReadyProcessOrder(orderData.getEntityId())
                .orElseThrow(() -> new EntityNotFoundException("Order not found"));

        OrderStatusEnum currentStatus = OrderStatusEnum.PROCESSING;

        BigDecimal totalSubtotal = BigDecimal.ZERO;
        boolean mutationHasError = false;
        boolean mutationFreeze = false;

        for (OrderReadyProcessDetailDto orderReadyProcessDetail : orderReadyProcess.getListDetail()) {
            totalSubtotal = totalSubtotal.add(orderReadyProcessDetail.subTotal());
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

            switch (mutationResult) {
                case QUANTITY_EXCEEDS :
                case RESERVED_QUANTITY_EXCEEDS : {
                    mutationHasError = true;
                    break;
                }
                case FREEZED: {
                    mutationFreeze = true;
                    break;
                }
            }
            if (mutationHasError || mutationFreeze) break;
        }

        if (mutationHasError) {
            userBalanceService.refund(
                new UserBalanceMutationDto(
                    BigDecimal.ZERO,
                    totalSubtotal.multiply(BigDecimal.valueOf(-1)),
                    UserBalanceLogTypeEnum.REFUND_STOCK_SHORTAGE,
                    orderReadyProcess.getUserId(),
                    null,
                    orderReadyProcess.getId()
                )
            );

            transitionOrderStatus(
                orderReadyProcess.getId(),
                currentStatus,
                OrderStatusEnum.CANCELLED
            );
            return;
        }

        if (mutationFreeze) {
            transitionOrderStatus(
                orderReadyProcess.getId(),
                currentStatus,
                OrderStatusEnum.PENDING
            );
            return;
        }

        transitionOrderStatus(
            orderReadyProcess.getId(),
            currentStatus,
            OrderStatusEnum.RESERVED
        );
    }

    private void transitionOrderStatus(
        UUID orderId,
        OrderStatusEnum expectedStatus,
        OrderStatusEnum newStatus
    ) {
        int updatedRows = orderRepository.updateOrderStatus(orderId, newStatus, expectedStatus);

        if (updatedRows != 1) {
            throw new IllegalStateException(
                String.format(
                    ORDER_STATUS_TRANSITION_FAILED_MESSAGE,
                    expectedStatus,
                    newStatus,
                    updatedRows,
                    orderId
                )
            );
        }
    }
}
