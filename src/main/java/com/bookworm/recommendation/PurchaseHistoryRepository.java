package com.bookworm.recommendation;

import java.util.List;

import com.bookworm.order.Order;
import com.bookworm.order.OrderStatus;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Read-only view of what a user has ordered, owned by the recommendation feature so that the order feature does
 * not need to know about it.
 */
public interface PurchaseHistoryRepository extends Repository<Order, Long> {

    /** Distinct books in the user's orders, ignoring cancelled ones. */
    @Query("""
            select distinct new com.bookworm.recommendation.PurchasedBook(b.id, b.category.id, b.author)
            from Order o join o.items i join i.book b
            where o.user.id = :userId and o.status <> :ignored
            """)
    List<PurchasedBook> findBooksOrderedBy(@Param("userId") Long userId, @Param("ignored") OrderStatus ignored);
}
