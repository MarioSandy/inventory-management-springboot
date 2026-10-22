package com.example.inventory_api_springboot.projection;

import com.example.inventory_api_springboot.enums.OutboxEventTypeEnum;
import tools.jackson.databind.JsonNode;

import java.util.UUID;

public interface OutboxPendingTriggerProjection {
    UUID getId();
    OutboxEventTypeEnum getEventType();
    UUID getEntityId();
    UUID getCreatedBy();
    String getPayload();
}
