package com.amin.beer.inventory.service.services;

import com.amin.beer.inventory.service.config.JmsConfig;
import com.amin.brewery.model.events.DeallocateOrderRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeallocationListener {
    private final AllocationService allocationService;

    @JmsListener(destination = JmsConfig.DEALLOCATE_ORDER_QUEUE)
    public void listen(DeallocateOrderRequest deallocateOrderRequest) {
        allocationService.deallocateOrder(deallocateOrderRequest.getBeerOrderDto());
    }
}
