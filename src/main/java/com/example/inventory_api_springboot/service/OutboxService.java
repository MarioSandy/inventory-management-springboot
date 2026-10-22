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
    private static final String OUTBOX_STATUS_UNCHANGED_MESSAGE =
        "Outbox status update affected %d row(s) instead of 1 for outbox id %s. " +
        "The outbox row likely does not exist or its status/lock guard is not satisfied.";

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

    @Transactional
    public void updateStatusFailedDispatch(UUID outboxId, String errorMessage) {
        int updatedRows = outboxRepository.updateStatusFailedDispatch(outboxId, errorMessage);
        assertSingleRowUpdated(updatedRows, outboxId);
    }

    @Transactional
    public void updateStatusForRetryDispatch(UUID outboxId, String errorMessage) {
        int updatedRows = outboxRepository.updateStatusForRetryDispatch(outboxId, errorMessage);
        assertSingleRowUpdated(updatedRows, outboxId);
    }

    @Transactional
    public void updateStatusSuccessDispatch(UUID outboxId) {
        int updatedRows = outboxRepository.updateStatusSuccessDispatch(outboxId);
        assertSingleRowUpdated(updatedRows, outboxId);
    }

    private void assertSingleRowUpdated(int updatedRows, UUID outboxId) {
        if (updatedRows != 1) {
            throw new IllegalStateException(
                String.format(OUTBOX_STATUS_UNCHANGED_MESSAGE, updatedRows, outboxId)
            );
        }
    }
}
