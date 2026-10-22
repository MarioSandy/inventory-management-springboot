package com.example.inventory_api_springboot.service;

import com.example.inventory_api_springboot.entity.Outbox;
import com.example.inventory_api_springboot.entity.User;
import com.example.inventory_api_springboot.enums.OutboxEventTypeEnum;
import com.example.inventory_api_springboot.enums.OutboxStatusEnum;
import com.example.inventory_api_springboot.projection.OutboxPendingTriggerProjection;
import com.example.inventory_api_springboot.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OutboxService {
    private final OutboxRepository outboxRepository;
    private final UserService userService;

    public void create(
        UUID entityId,
        String entity,
        OutboxEventTypeEnum type,
        JsonNode payload,
        UUID userId
    )  {
        User userProxy = null;
        if (userId != null) userProxy = userService.getReferenceById(userId);

        Outbox outbox = Outbox.builder()
            .entityId(entityId)
            .entity(entity)
            .eventType(type)
            .status(OutboxStatusEnum.PENDING)
            .payload(payload)
            .user(userProxy)
            .build();

        outboxRepository.save(outbox);
    }

    @Transactional
    public List<OutboxPendingTriggerProjection> getPendingOutbox() {
        return outboxRepository.getAndMarkedPendingOutbox();
    }

    public void updateStatusFailedDispatch(UUID outboxId, String errorMessage) {
        outboxRepository.updateStatusFailedDispatch(outboxId, errorMessage);
    }

    public void updateStatusForRetryDispatch(UUID outboxId, String errorMessage) {
        outboxRepository.updateStatusForRetryDispatch(outboxId, errorMessage);
    }

    public void updateStatusSuccessDispatch(UUID outboxId) {
        outboxRepository.updateStatusSuccessDispatch(outboxId);
    }
}
