package com.bookworm.order;

import com.bookworm.address.AddressUsageChecker;

import org.springframework.stereotype.Component;

/**
 * Prevents deleting an address that an order was shipped to (or will be), so order history stays intact.
 */
@Component
class OrderAddressUsageChecker implements AddressUsageChecker {

    private final OrderRepository orderRepository;

    OrderAddressUsageChecker(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public boolean isInUse(Long addressId) {
        return orderRepository.existsByAddressId(addressId);
    }
}
