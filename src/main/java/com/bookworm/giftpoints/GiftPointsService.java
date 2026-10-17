package com.bookworm.giftpoints;

import java.math.BigDecimal;

import com.bookworm.common.error.BusinessRuleException;
import com.bookworm.user.User;
import com.bookworm.user.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Previews gift point redemption without changing anything; the real redemption happens when an order is placed.
 */
@Service
@Transactional(readOnly = true)
public class GiftPointsService {

    private final UserRepository userRepository;
    private final GiftPointsPolicy policy;

    public GiftPointsService(UserRepository userRepository, GiftPointsPolicy policy) {
        this.userRepository = userRepository;
        this.policy = policy;
    }

    /**
     * @throws BusinessRuleException if the user has fewer points than requested, or the points would be worth
     *                               more than the subtotal
     */
    public GiftPointsRedemptionResponse previewRedemption(Long userId, GiftPointsRedemptionRequest request) {
        User user = userRepository.getAuthenticated(userId);
        int points = request.pointsToRedeem();
        if (points > user.getGiftPoints()) {
            throw new BusinessRuleException(
                    "Cannot redeem %d gift points; only %d are available.".formatted(points, user.getGiftPoints()));
        }
        policy.requireWithinSubtotal(points, request.orderSubtotal());

        BigDecimal discount = policy.discountFor(points);
        return new GiftPointsRedemptionResponse(points, discount, user.getGiftPoints() - points,
                request.orderSubtotal().subtract(discount));
    }
}
