package com.example.inventory_api_springboot.scheduler;

import com.example.inventory_api_springboot.queue.publisher.MessagePublisher;
import com.example.inventory_api_springboot.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OrderScheduler {
    private final OrderService orderService;

    @Scheduled(fixedRate = 60000)
    public void checkReadyOrderAndPushToOutbox() {
        orderService.checkReadyOrderAndPushToOutbox();
    }
}
