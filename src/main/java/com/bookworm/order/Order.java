package com.bookworm.order;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.bookworm.address.Address;
import com.bookworm.book.BookQuantity;
import com.bookworm.common.error.BusinessRuleException;
import com.bookworm.pricing.PriceBreakdown;
import com.bookworm.user.User;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

/**
 * A placed order. Creating one ({@link #place}) takes stock and gift points; cancelling one ({@link #cancel})
 * gives them back, so those two rules always stay symmetrical.
 */
@Entity
@Table(name = "orders", indexes = @Index(name = "idx_orders_user_created", columnList = "user_id, created_at"))
public class Order {

    /** How long after placement a customer may still cancel. */
    public static final Duration CANCELLATION_WINDOW = Duration.ofHours(48);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "address_id", nullable = false)
    private Address address;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal tax;

    @Column(name = "delivery_charges", nullable = false, precision = 10, scale = 2)
    private BigDecimal deliveryCharges;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal discount;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "gift_points_redeemed", nullable = false)
    private int giftPointsRedeemed;

    @Column(name = "gift_points_earned", nullable = false)
    private int giftPointsEarned;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<OrderItem> items = new ArrayList<>();

    protected Order() {
    }

    private Order(User user, Address address, PriceBreakdown prices, int giftPointsRedeemed, BigDecimal discount,
                  int giftPointsEarned, Instant createdAt) {
        this.user = user;
        this.address = address;
        this.status = OrderStatus.PENDING_PAYMENT;
        this.subtotal = prices.subtotal();
        this.tax = prices.tax();
        this.deliveryCharges = prices.deliveryCharges();
        this.discount = discount;
        this.totalAmount = prices.totalAfter(discount);
        this.giftPointsRedeemed = giftPointsRedeemed;
        this.giftPointsEarned = giftPointsEarned;
        this.createdAt = createdAt;
    }

    /**
     * Places an order: reserves stock for every line and spends the redeemed gift points.
     *
     * @throws BusinessRuleException if any book lacks stock or the user lacks the gift points
     */
    public static Order place(User user, Address address, List<BookQuantity> lines, PriceBreakdown prices,
                              int giftPointsRedeemed, BigDecimal discount, int giftPointsEarned, Instant now) {
        Order order = new Order(user, address, prices, giftPointsRedeemed, discount, giftPointsEarned, now);
        user.deductGiftPoints(giftPointsRedeemed);
        for (BookQuantity line : lines) {
            line.book().reserveStock(line.quantity());
            order.items.add(new OrderItem(order, line.book(), line.quantity()));
        }
        return order;
    }

    /**
     * Cancels the order, returning stock and any redeemed gift points.
     *
     * @throws BusinessRuleException if the order has progressed past cancellation or the window has closed
     */
    public void cancel(Instant now) {
        if (!status.isCancellable()) {
            throw new BusinessRuleException("Order cannot be cancelled because its status is " + status + ".");
        }
        if (now.isAfter(cancellationDeadline())) {
            throw new BusinessRuleException("The 48-hour cancellation window for this order has expired.");
        }
        status = OrderStatus.CANCELLED;
        items.forEach(item -> item.getBook().restock(item.getQuantity()));
        user.addGiftPoints(giftPointsRedeemed);
    }

    public Instant cancellationDeadline() {
        return createdAt.plus(CANCELLATION_WINDOW);
    }

    public boolean isOwnedBy(Long userId) {
        return user.getId().equals(userId);
    }

    /** The books and quantities in this order, e.g. to add them to a cart again. */
    public List<BookQuantity> lines() {
        return items.stream().map(item -> new BookQuantity(item.getBook(), item.getQuantity())).toList();
    }

    public Long getId() {
        return id;
    }

    public Address getAddress() {
        return address;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getTax() {
        return tax;
    }

    public BigDecimal getDeliveryCharges() {
        return deliveryCharges;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public int getGiftPointsRedeemed() {
        return giftPointsRedeemed;
    }

    public int getGiftPointsEarned() {
        return giftPointsEarned;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<OrderItem> getItems() {
        return List.copyOf(items);
    }
}
