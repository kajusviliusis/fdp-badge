package com.bookworm.order;

import java.time.Clock;

import com.bookworm.cart.CartResponse;
import com.bookworm.cart.CartService;
import com.bookworm.common.error.ForbiddenException;
import com.bookworm.common.error.ResourceNotFoundException;
import com.bookworm.common.page.PageResponse;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Order history and the actions available on an existing order. Placing orders is in {@link CheckoutService}.
 */
@Service
@Transactional(readOnly = true)
public class OrderService {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private final OrderRepository orderRepository;
    private final CartService cartService;
    private final Clock clock;

    public OrderService(OrderRepository orderRepository, CartService cartService, Clock clock) {
        this.orderRepository = orderRepository;
        this.cartService = cartService;
        this.clock = clock;
    }

    /**
     * @param status optional filter; {@code null} lists orders in every status
     */
    public PageResponse<OrderResponse> list(Long userId, OrderStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, NEWEST_FIRST);
        Page<Order> orders = status == null
                ? orderRepository.findByUserId(userId, pageable)
                : orderRepository.findByUserIdAndStatus(userId, status, pageable);
        return PageResponse.of(orders, OrderResponse::from);
    }

    public OrderResponse get(Long userId, Long orderId) {
        return OrderResponse.from(getOwned(userId, orderId));
    }

    @Transactional
    public OrderResponse cancel(Long userId, Long orderId) {
        Order order = getOwned(userId, orderId);
        order.cancel(clock.instant());
        return OrderResponse.from(order);
    }

    /**
     * Puts the books of a past order back into the user's cart. Books that are out of stock, or would exceed
     * the per-book limit, are skipped rather than failing the whole request.
     */
    @Transactional
    public CartResponse buyAgain(Long userId, Long orderId) {
        return cartService.addAvailableBooks(userId, getOwned(userId, orderId).lines());
    }

    /**
     * @throws ResourceNotFoundException if the order does not exist
     * @throws ForbiddenException        if it belongs to another user
     */
    Order getOwned(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> ResourceNotFoundException.of("Order", orderId));
        if (!order.isOwnedBy(userId)) {
            throw new ForbiddenException("You do not have access to this order.");
        }
        return order;
    }
}
