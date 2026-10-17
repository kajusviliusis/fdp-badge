package com.bookworm.payment;

import java.math.BigDecimal;

/**
 * Port to whatever actually moves the money. Payments are simulated for now ({@link SimulatedPaymentGateway});
 * a real provider only needs to implement this interface, and no other code changes.
 */
public interface PaymentGateway {

    /**
     * Attempts to charge {@code amount} using the details in {@code request}. A declined payment is a normal
     * outcome and is reported through the result, not an exception.
     */
    PaymentResult charge(PaymentRequest request, BigDecimal amount);
}
