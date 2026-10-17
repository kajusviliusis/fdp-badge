package com.bookworm.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.bookworm.address.AddressResponse;
import com.bookworm.payment.Payment;
import com.bookworm.payment.PaymentResponse;

public record OrderResponse(
        Long id,
        OrderStatus status,
        List<OrderItemResponse> items,
        AddressResponse address,
        BigDecimal subtotal,
        BigDecimal tax,
        BigDecimal deliveryCharges,
        BigDecimal discount,
        BigDecimal totalAmount,
        int giftPointsRedeemed,
        int giftPointsEarned,
        Instant createdAt,
        Instant cancellationDeadline,
        PaymentResponse payment) {

    /**
     * @param payment the order's payment, or {@code null} if none has been attempted (omitted from the JSON)
     */
    static OrderResponse from(Order order, Payment payment) {
        return new OrderResponse(
                order.getId(),
                order.getStatus(),
                order.getItems().stream().map(OrderItemResponse::from).toList(),
                AddressResponse.from(order.getAddress()),
                order.getSubtotal(),
                order.getTax(),
                order.getDeliveryCharges(),
                order.getDiscount(),
                order.getTotalAmount(),
                order.getGiftPointsRedeemed(),
                order.getGiftPointsEarned(),
                order.getCreatedAt(),
                order.cancellationDeadline(),
                payment == null ? null : PaymentResponse.from(payment));
    }
}
