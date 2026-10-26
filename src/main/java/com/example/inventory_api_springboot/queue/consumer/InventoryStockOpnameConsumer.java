package com.example.inventory_api_springboot.queue.consumer;

import com.example.inventory_api_springboot.config.RabbitMQConfig;
import com.example.inventory_api_springboot.dto.queue.OutboxPublisherMessageDto;
import com.example.inventory_api_springboot.service.InventoryService;
import com.example.inventory_api_springboot.service.StockOpnameService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InventoryStockOpnameConsumer {
    private final StockOpnameService stockOpnameService;

    @RabbitListener(queues = RabbitMQConfig.INVENTORY_STOCK_OPNAME_QUEUE, concurrency = "5")
    public void processProductBatchCreate(OutboxPublisherMessageDto inventoryData) {
        try {
            stockOpnameService.excelStockOpname(inventoryData);
        } catch (org.springframework.dao.DataAccessException e) {
            throw new RuntimeException("Transient error, retry allowed", e);

        } catch (NullPointerException | IllegalStateException | IllegalArgumentException e) {
            throw new org.springframework.amqp.AmqpRejectAndDontRequeueException("Fatal payload/parsing error", e);
        } catch (Exception e) {
            throw new org.springframework.amqp.AmqpRejectAndDontRequeueException("Unexpected critical error", e);
        }
    }
}
