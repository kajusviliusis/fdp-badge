package com.bookworm.payment;

import com.bookworm.common.error.BusinessRuleException;
import com.bookworm.common.error.ResourceNotFoundException;
import com.bookworm.order.Order;
import com.bookworm.order.OrderService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Takes payment for an order. A successful charge confirms the order; a declined one leaves it awaiting payment
 * so the customer can try again with another method.
 */
@Service
@Transactional(readOnly = true)
public class PaymentService {

    private final OrderService orderService;
    private final PaymentRepository paymentRepository;
    private final PaymentGateway paymentGateway;

    public PaymentService(OrderService orderService, PaymentRepository paymentRepository,
                          PaymentGateway paymentGateway) {
        this.orderService = orderService;
        this.paymentRepository = paymentRepository;
        this.paymentGateway = paymentGateway;
    }

    /**
     * Charges the order's total. Always returns the outcome of the attempt (HTTP 200), including declines, which
     * are reported as {@link PaymentStatus#FAILED}.
     * <p>
     * The order is locked for the duration of the transaction, so a double-submitted request cannot charge twice:
     * the loser fails with an optimistic locking error.
     *
     * @throws BusinessRuleException if the order is not awaiting payment, or was already paid
     */
    @Transactional
    public PaymentResponse pay(Long userId, Long orderId, PaymentRequest request) {
        Order order = orderService.getOwnedForUpdate(userId, orderId);
        order.requireAwaitingPayment();

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseGet(() -> new Payment(order, request.method(), order.getTotalAmount()));
        PaymentResult result = paymentGateway.charge(request, order.getTotalAmount());
        payment.record(request.method(), result);
        if (result.approved()) {
            order.confirmPayment();
        }
        return PaymentResponse.from(paymentRepository.save(payment));
    }

    /**
     * @throws ResourceNotFoundException if the order does not exist, or has no payment yet
     */
    public PaymentResponse get(Long userId, Long orderId) {
        orderService.getOwned(userId, orderId);
        return paymentRepository.findByOrderId(orderId)
                .map(PaymentResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No payment has been made for order %d.".formatted(orderId)));
    }
}
