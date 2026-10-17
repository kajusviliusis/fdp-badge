package com.bookworm.order;

import com.bookworm.cart.CartResponse;
import com.bookworm.common.page.PageResponse;
import com.bookworm.common.security.CurrentUserId;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final CheckoutService checkoutService;
    private final OrderService orderService;

    public OrderController(CheckoutService checkoutService, OrderService orderService) {
        this.checkoutService = checkoutService;
        this.orderService = orderService;
    }

    @GetMapping
    public PageResponse<OrderResponse> list(
            @CurrentUserId Long userId,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size) {
        return orderService.list(userId, status, page, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(@CurrentUserId Long userId, @Valid @RequestBody CreateOrderRequest request) {
        return checkoutService.placeOrder(userId, request);
    }

    @GetMapping("/{orderId}")
    public OrderResponse get(@CurrentUserId Long userId, @PathVariable Long orderId) {
        return orderService.get(userId, orderId);
    }

    @PostMapping("/{orderId}/cancel")
    public OrderResponse cancel(@CurrentUserId Long userId, @PathVariable Long orderId) {
        return orderService.cancel(userId, orderId);
    }

    @PostMapping("/{orderId}/buy-again")
    public CartResponse buyAgain(@CurrentUserId Long userId, @PathVariable Long orderId) {
        return orderService.buyAgain(userId, orderId);
    }
}
