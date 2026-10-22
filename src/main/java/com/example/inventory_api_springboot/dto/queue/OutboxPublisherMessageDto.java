package com.example.inventory_api_springboot.dto.queue;

import tools.jackson.databind.JsonNode;

import java.io.Serializable;
import java.util.UUID;

public record OutboxPublisherMessageDto(
    UUID getId,
    UUID getEntityId,
    JsonNode getPayload
) implements Serializable {}
