package com.example.inventory_api_springboot.queue.consumer;

import com.example.inventory_api_springboot.config.RabbitMQConfig;
import com.example.inventory_api_springboot.dto.queue.OutboxPublisherMessageDto;
import com.example.inventory_api_springboot.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductBatchCreateConsumer {
    private final ProductService productService;

    @RabbitListener(queues = RabbitMQConfig.PRODUCT_BATCH_CREATE_QUEUE, concurrency = "5")
    public void processProductBatchCreate(OutboxPublisherMessageDto productData) {
        try {
            productService.insertProductBatch(productData);
        } catch (org.springframework.dao.DataAccessException e) {
            throw new RuntimeException("Transient error, retry allowed", e);
        } catch (NullPointerException | IllegalStateException | IllegalArgumentException e) {
            throw new org.springframework.amqp.AmqpRejectAndDontRequeueException("Fatal payload/parsing error", e);
        } catch (Exception e) {
            throw new org.springframework.amqp.AmqpRejectAndDontRequeueException("Unexpected critical error", e);
        }
    }
}
