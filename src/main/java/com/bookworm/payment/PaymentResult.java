package com.bookworm.payment;

/**
 * Outcome of a charge attempt.
 *
 * @param transactionReference identifier of the attempt, present whether or not it was approved
 */
public record PaymentResult(boolean approved, String transactionReference) {
}
