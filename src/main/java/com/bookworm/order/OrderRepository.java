package com.bookworm.order;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * Loads an order and bumps its version on commit, so two requests that load it at the same time cannot both
     * succeed: the second fails with an optimistic locking error even if it changed nothing itself.
     */
    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    @Query("select o from Order o where o.id = :id")
    Optional<Order> findByIdForUpdate(@Param("id") Long id);

    Page<Order> findByUserId(Long userId, Pageable pageable);

    Page<Order> findByUserIdAndStatus(Long userId, OrderStatus status, Pageable pageable);

    boolean existsByAddressId(Long addressId);

    /** Whether the user has an order in one of the given statuses that contains the book. */
    @Query("""
            select count(o) > 0 from Order o join o.items i
            where o.user.id = :userId and i.book.id = :bookId and o.status in :statuses
            """)
    boolean existsContainingBook(@Param("userId") Long userId, @Param("bookId") Long bookId,
                                 @Param("statuses") Collection<OrderStatus> statuses);

    /**
     * Ids of the books with the most copies in orders in one of the given statuses that were paid in
     * {@code [from, until)}, most copies first.
     */
    @Query("""
            select i.book.id from OrderItem i
            where i.order.status in :statuses and i.order.paidAt >= :from and i.order.paidAt < :until
            group by i.book.id
            order by sum(i.quantity) desc, i.book.id asc
            """)
    List<Long> findTopSellingBookIds(@Param("statuses") Collection<OrderStatus> statuses,
                                     @Param("from") Instant from, @Param("until") Instant until,
                                     Pageable pageable);
}
