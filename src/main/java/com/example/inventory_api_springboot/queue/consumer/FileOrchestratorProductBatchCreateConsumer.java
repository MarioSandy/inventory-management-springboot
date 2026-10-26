package com.example.inventory_api_springboot.queue.consumer;

import com.example.inventory_api_springboot.config.RabbitMQConfig;
import com.example.inventory_api_springboot.dto.queue.OutboxPublisherMessageDto;
import com.example.inventory_api_springboot.service.FileOrchestratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FileOrchestratorProductBatchCreateConsumer {
    private final FileOrchestratorService fileOrchestratorService;

    @RabbitListener(queues = RabbitMQConfig.FILE_ORCHESTRA_PRODUCT_BATCH_CREATE_QUEUE, concurrency = "3")
    public void orchestrate(
        OutboxPublisherMessageDto outboxData
    ) {
        try {
            fileOrchestratorService.chunkCreateBatchProductExcelFile(outboxData);
        } catch (org.springframework.dao.DataAccessException | java.io.IOException e) {
            throw new RuntimeException("Transient error, retry allowed", e);
        } catch (NullPointerException | IllegalStateException | IllegalArgumentException e) {
            throw new org.springframework.amqp.AmqpRejectAndDontRequeueException("Fatal payload/parsing error", e);
        } catch (Exception e) {
            throw new org.springframework.amqp.AmqpRejectAndDontRequeueException("Unexpected critical error", e);
        }
    }
}
