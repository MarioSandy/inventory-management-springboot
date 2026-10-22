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
            System.out.println("File Orchestrator Product Batch Queue");
            // Execute your Excel parsing logic
            fileOrchestratorService.chunkCreateBatchProductExcelFile(outboxData);
            System.out.println("File Orchestrator Product Batch Queue succeed");
        } catch (org.springframework.dao.DataAccessException | java.io.IOException e) {
            // 1. INFRASTRUCTURE ERRORS (S3 Down, DB Connection dropped, Network Blip)
            throw new RuntimeException("Transient error, retry allowed", e);

        } catch (NullPointerException | IllegalStateException | IllegalArgumentException e) {
            // 2. BAD DATA / BROKEN CODE (User uploaded empty cell, wrong column format, invalid file format)
            throw new org.springframework.amqp.AmqpRejectAndDontRequeueException("Fatal payload/parsing error", e);

        } catch (Exception e) {
            // 3. FALLBACK CATCHALL
            throw new org.springframework.amqp.AmqpRejectAndDontRequeueException("Unexpected critical error", e);
        }
    }
}
