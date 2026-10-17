package com.bookworm.payment;

import com.bookworm.common.security.CurrentUserId;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders/{orderId}/payment")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public PaymentResponse pay(@CurrentUserId Long userId, @PathVariable Long orderId,
                               @Valid @RequestBody PaymentRequest request) {
        return paymentService.pay(userId, orderId, request);
    }

    @GetMapping
    public PaymentResponse get(@CurrentUserId Long userId, @PathVariable Long orderId) {
        return paymentService.get(userId, orderId);
    }
}
