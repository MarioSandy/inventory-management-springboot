package com.example.inventory_api_springboot.scheduler;

import com.example.inventory_api_springboot.queue.publisher.MessagePublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxScheduler {
    private final MessagePublisher messagePublisher;

    @Scheduled(fixedRate = 60000)
    public void checkAndPushOutbox() {
        messagePublisher.checkAndTriggerPendingMessage();
    }

    @Scheduled(fixedRate = 60000)
    public void checkAndPushOrderOutbox() {
        messagePublisher.checkAndTriggerPendingOrderProcessingMessage();
    }
}
