package com.example.inventory_api_springboot.queue.consumer;

import com.example.inventory_api_springboot.config.RabbitMQConfig;
import com.example.inventory_api_springboot.dto.queue.OutboxPublisherMessageDto;
import com.example.inventory_api_springboot.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderProcessingConsumer {
    private final OrderService orderService;

    @RabbitListener(queues = RabbitMQConfig.ORDER_PROCESSING_QUEUE, concurrency = "1")
    public void processOrder(OutboxPublisherMessageDto orderData) {
        try {
            orderService.processOrder(orderData);
        } catch (org.springframework.dao.DataAccessException e) {
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
