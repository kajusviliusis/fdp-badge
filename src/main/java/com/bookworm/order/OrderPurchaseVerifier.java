package com.bookworm.order;

import java.util.Arrays;
import java.util.List;

import com.bookworm.review.PurchaseVerifier;

import org.springframework.stereotype.Component;

/**
 * A book counts as purchased once an order containing it has been paid and not cancelled.
 */
@Component
class OrderPurchaseVerifier implements PurchaseVerifier {

    private static final List<OrderStatus> PAID_STATUSES =
            Arrays.stream(OrderStatus.values()).filter(OrderStatus::isPaid).toList();

    private final OrderRepository orderRepository;

    OrderPurchaseVerifier(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public boolean hasPurchased(Long userId, Long bookId) {
        return orderRepository.existsContainingBook(userId, bookId, PAID_STATUSES);
    }
}
