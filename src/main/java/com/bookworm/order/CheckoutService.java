package com.bookworm.order;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

import com.bookworm.address.Address;
import com.bookworm.address.AddressService;
import com.bookworm.book.BookQuantity;
import com.bookworm.cart.CartService;
import com.bookworm.common.error.BusinessRuleException;
import com.bookworm.coupon.AppliedCoupon;
import com.bookworm.coupon.CouponService;
import com.bookworm.giftpoints.GiftPointsPolicy;
import com.bookworm.pricing.PriceBreakdown;
import com.bookworm.pricing.PricingService;
import com.bookworm.user.User;
import com.bookworm.user.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Turns a user's cart into an order. Everything happens in one transaction: if any step fails (no stock,
 * too many gift points, ...) the cart, stock and gift points are left exactly as they were.
 */
@Service
public class CheckoutService {

    private final CartService cartService;
    private final AddressService addressService;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final PricingService pricingService;
    private final GiftPointsPolicy giftPointsPolicy;
    private final CouponService couponService;
    private final Clock clock;

    public CheckoutService(CartService cartService, AddressService addressService, UserRepository userRepository,
                           OrderRepository orderRepository, PricingService pricingService,
                           GiftPointsPolicy giftPointsPolicy, CouponService couponService, Clock clock) {
        this.cartService = cartService;
        this.addressService = addressService;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.pricingService = pricingService;
        this.giftPointsPolicy = giftPointsPolicy;
        this.couponService = couponService;
        this.clock = clock;
    }

    /**
     * Places an order for everything in the user's cart; the cart is emptied on success.
     * <p>
     * Discounts are applied to the subtotal only (tax and delivery are always paid in full): the coupon first,
     * then gift points, which may cover no more than what the coupon leaves of the subtotal.
     *
     * @throws com.bookworm.common.error.ResourceNotFoundException if the address is not one of the user's
     * @throws BusinessRuleException                               if the cart is empty, a book is out of
     *                                                             stock, or the gift point or coupon request
     *                                                             is invalid
     */
    @Transactional
    public OrderResponse placeOrder(Long userId, CreateOrderRequest request) {
        User user = userRepository.getAuthenticated(userId);
        Address address = addressService.getForCheckout(userId, request.addressId());

        List<BookQuantity> lines = cartService.drain(userId);
        if (lines.isEmpty()) {
            throw new BusinessRuleException("Cannot place an order with an empty cart.");
        }

        BigDecimal subtotal = lines.stream().map(BookQuantity::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        PriceBreakdown prices = pricingService.calculate(subtotal);
        Instant now = clock.instant();

        AppliedCoupon coupon = request.hasCoupon()
                ? couponService.redeem(request.couponCode(), prices.subtotal(), now)
                : null;
        BigDecimal couponDiscount = coupon == null ? BigDecimal.ZERO.setScale(2) : coupon.discount();

        int points = request.pointsToRedeem();
        giftPointsPolicy.requireWithinSubtotal(points, prices.subtotal().subtract(couponDiscount));

        BigDecimal discount = couponDiscount.add(giftPointsPolicy.discountFor(points));
        int pointsEarned = giftPointsPolicy.pointsEarnedFor(prices.totalAfter(discount));

        Order order = Order.place(user, address, lines, prices, points, discount, pointsEarned, now, coupon);
        return OrderResponse.from(orderRepository.save(order), null);
    }
}
