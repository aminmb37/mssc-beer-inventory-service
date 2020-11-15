package com.amin.beer.inventory.service.services;

import com.amin.beer.inventory.service.domain.BeerInventory;
import com.amin.beer.inventory.service.repositories.BeerInventoryRepository;
import com.amin.brewery.model.BeerOrderDto;
import com.amin.brewery.model.BeerOrderLineDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class AllocationServiceImpl implements AllocationService {
    private final BeerInventoryRepository beerInventoryRepository;

    @Override
    public Boolean allocateOrder(BeerOrderDto beerOrderDto) {
        log.debug("Allocating OrderId: " + beerOrderDto.getId());
        AtomicInteger totalOrdered = new AtomicInteger();
        AtomicInteger totalAllocated = new AtomicInteger();
        beerOrderDto.getBeerOrderLines().forEach(beerOrderLineDto -> {
            int orderQuantity = beerOrderLineDto.getOrderQuantity() != null ? beerOrderLineDto.getOrderQuantity() : 0;
            int quantityAllocated =
                    beerOrderLineDto.getQuantityAllocated() != null ? beerOrderLineDto.getQuantityAllocated() : 0;
            if (orderQuantity > quantityAllocated) {
                allocateBeerOrderLine(beerOrderLineDto);
            }
            totalOrdered.set(totalOrdered.get() + orderQuantity);
            totalAllocated.set(totalAllocated.get() + quantityAllocated);
        });
        log.debug("Total Ordered: " + totalOrdered.get() + " Total Allocated: " + totalAllocated.get());
        return totalOrdered.get() == totalAllocated.get();
    }

    private void allocateBeerOrderLine(BeerOrderLineDto beerOrderLineDto) {
        int orderQty = beerOrderLineDto.getOrderQuantity() != null ? beerOrderLineDto.getOrderQuantity() : 0;
        List<BeerInventory> beerInventoryList = beerInventoryRepository.findAllByUpc(beerOrderLineDto.getUpc());
        beerInventoryList.forEach(beerInventory -> {
            int inventory = beerInventory.getQuantityOnHand() != null ? beerInventory.getQuantityOnHand() : 0;
            int allocatedQty =
                    beerOrderLineDto.getQuantityAllocated() != null ? beerOrderLineDto.getQuantityAllocated() : 0;
            int qtyToAllocate = orderQty - allocatedQty;
            if (inventory > qtyToAllocate) {
                inventory -= qtyToAllocate;
                beerOrderLineDto.setQuantityAllocated(orderQty);
                beerInventory.setQuantityOnHand(inventory);
                beerInventoryRepository.save(beerInventory);
            } else if (inventory > 0) {
                beerOrderLineDto.setQuantityAllocated(allocatedQty + inventory);
                beerInventory.setQuantityOnHand(0);
                beerInventoryRepository.delete(beerInventory);
            } else {
                beerInventoryRepository.delete(beerInventory);
            }
        });
    }

    @Override
    public void deallocateOrder(BeerOrderDto beerOrderDto) {
        beerOrderDto.getBeerOrderLines().forEach(beerOrderLineDto -> {
            BeerInventory beerInventory = BeerInventory.builder().beerId(beerOrderLineDto.getBeerId())
                    .upc(beerOrderLineDto.getUpc()).quantityOnHand(beerOrderLineDto.getQuantityAllocated()).build();
            BeerInventory savedBeerInventory = beerInventoryRepository.save(beerInventory);
            log.debug("Saved Inventory for beer upc: " +
                    savedBeerInventory.getUpc() + " inventory id: " + savedBeerInventory.getId());
        });
    }
}
