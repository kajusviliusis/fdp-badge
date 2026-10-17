package com.bookworm.order;

import java.util.Collection;
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
}
