package com.bookworm.order;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import com.bookworm.book.SalesReport;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sales are copies in orders that have been paid and not cancelled, attributed to the time of payment.
 */
@Component
class OrderSalesReport implements SalesReport {

    private static final List<OrderStatus> PAID_STATUSES =
            Arrays.stream(OrderStatus.values()).filter(OrderStatus::isPaid).toList();

    private final OrderRepository orderRepository;

    OrderSalesReport(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> topSellingBookIds(Instant from, Instant until, int limit) {
        return orderRepository.findTopSellingBookIds(PAID_STATUSES, from, until, PageRequest.of(0, limit));
    }
}
