package com.example.inventory_api_springboot.repository.order;

import com.example.inventory_api_springboot.dto.query.response.OrderReadyProcessDto;
import com.example.inventory_api_springboot.dto.response.OrderDetailCancelResponseDto;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderNativeRepository {
    List<OrderDetailCancelResponseDto>  cancelOrderDetail(
        UUID productVariantId,
        int shortage
    );

    List<UUID> getReadyPendingOrderIdsForProcess();
    Optional<OrderReadyProcessDto> getReadyProcessOrder(UUID id);
}
