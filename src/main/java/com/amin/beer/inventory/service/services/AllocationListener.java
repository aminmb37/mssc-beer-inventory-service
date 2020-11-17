package com.amin.beer.inventory.service.services;

import com.amin.beer.inventory.service.config.JmsConfig;
import com.amin.brewery.model.events.AllocateOrderRequest;
import com.amin.brewery.model.events.AllocateOrderResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AllocationListener {
    private final JmsTemplate jmsTemplate;
    private final AllocationService allocationService;

    @JmsListener(destination = JmsConfig.ALLOCATE_ORDER_QUEUE)
    public void listen(AllocateOrderRequest allocateOrderRequest) {
        AllocateOrderResult.AllocateOrderResultBuilder allocateOrderResultBuilder = AllocateOrderResult.builder();
        try {
            Boolean allocationResult = allocationService.allocateOrder(allocateOrderRequest.getBeerOrderDto());
            allocateOrderResultBuilder.beerOrderDto(allocateOrderRequest.getBeerOrderDto());
            allocateOrderResultBuilder.pendingInventory(!allocationResult);
            allocateOrderResultBuilder.allocationError(false);
        } catch (Exception e) {
            log.error("Allocation failed for Order Id: " + allocateOrderRequest.getBeerOrderDto().getId());
            allocateOrderResultBuilder.allocationError(true);
        }
        jmsTemplate.convertAndSend(JmsConfig.ALLOCATE_ORDER_RESPONSE_QUEUE, allocateOrderResultBuilder.build());
    }
}
