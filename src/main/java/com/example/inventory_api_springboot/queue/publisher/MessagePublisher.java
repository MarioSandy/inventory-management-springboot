package com.example.inventory_api_springboot.queue.publisher;

import com.example.inventory_api_springboot.config.RabbitMQConfig;
import com.example.inventory_api_springboot.dto.queue.OutboxPublisherMessageDto;
import com.example.inventory_api_springboot.enums.OutboxEventTypeEnum;
import com.example.inventory_api_springboot.projection.OutboxPendingTriggerProjection;
import com.example.inventory_api_springboot.service.OutboxService;
import com.nimbusds.jose.shaded.gson.Gson;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MessagePublisher {
    private final RabbitTemplate rabbitTemplate;
    private final OutboxService outboxService;
    private final ObjectMapper objectMapper;

    public void checkAndTriggerPendingMessage() {
        List<OutboxEventTypeEnum> listForbiddenType = Arrays.asList(OutboxEventTypeEnum.ORDER_PROCESSING);

        List<OutboxPendingTriggerProjection> listPendingOutbox = outboxService.getPendingOutbox(listForbiddenType);

        for (OutboxPendingTriggerProjection pendingOutbox : listPendingOutbox) {
            triggerMessage(pendingOutbox);
        }
    }

    public void checkAndTriggerPendingOrderProcessingMessage() {
        List<OutboxPendingTriggerProjection> listPendingOutbox = outboxService.getPendingOrderOutbox();

        for (OutboxPendingTriggerProjection pendingOutbox : listPendingOutbox) {
            triggerMessage(pendingOutbox);
        }
    }

    public void triggerMessage(
        OutboxPendingTriggerProjection pendingOutboxData
    ) {
        String exchangeName = null;
        String routingKey = null;

        try {
            switch(pendingOutboxData.getEventType()) {
                case FILE_ORCHESTRA_PRODUCT_BATCH_CREATE: {
                    exchangeName = RabbitMQConfig.FILE_ORCHESTRA_EXCHANGE;
                    routingKey = "file-orchestra.product.create-batch";
                    break;
                }
                case FILE_ORCHESTRA_INVENTORY_STOCK_OPNAME: {
                    exchangeName = RabbitMQConfig.FILE_ORCHESTRA_EXCHANGE;
                    routingKey = "file-orchestra.inventory.stock-opname";
                    break;
                }
                case CREATE_PRODUCT_BATCH : {
                    exchangeName = RabbitMQConfig.PRODUCT_EXCHANGE;
                    routingKey = "product.create-batch";
                    break;
                }
                case STOCK_OPNAME : {
                    exchangeName = RabbitMQConfig.INVENTORY_EXCHANGE;
                    routingKey = "inventory.stock-opname";
                    break;
                }
                case ORDER_PROCESSING: {
                    exchangeName = RabbitMQConfig.ORDER_EXCHANGE;
                    routingKey = "order.process";
                    break;
                }
            }

            OutboxPublisherMessageDto outboxPublisherMessage = new OutboxPublisherMessageDto(
                pendingOutboxData.getId(),
                pendingOutboxData.getEntityId(),
                pendingOutboxData.getPayload() != null
                    ? objectMapper.readTree(pendingOutboxData.getPayload())
                    : null
            );

            rabbitTemplate.convertAndSend(
                exchangeName,
                routingKey,
                outboxPublisherMessage
            );

            outboxService.updateStatusSuccessDispatch(pendingOutboxData.getId());
        } catch (MessageConversionException e) {
            e.printStackTrace();
            outboxService.updateStatusFailedDispatch(pendingOutboxData.getId(), e.getMessage());
        } catch (AmqpException e) {
            e.printStackTrace();
            outboxService.updateStatusForRetryDispatch(pendingOutboxData.getId(), e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
