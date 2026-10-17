package com.bookworm.payment;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderId(Long orderId);

    /** Loads the payments of many orders in one query, e.g. for an order history page. */
    List<Payment> findByOrderIdIn(Collection<Long> orderIds);
}
