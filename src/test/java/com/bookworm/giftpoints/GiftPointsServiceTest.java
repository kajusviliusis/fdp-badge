package com.bookworm.giftpoints;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import com.bookworm.common.error.BusinessRuleException;
import com.bookworm.common.error.UnauthorizedException;
import com.bookworm.user.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({GiftPointsService.class, GiftPointsPolicy.class, GiftPointsServiceTest.TestBeans.class})
class GiftPointsServiceTest {

    @TestConfiguration
    static class TestBeans {
        @Bean
        GiftPointsProperties giftPointsProperties() {
            return new GiftPointsProperties(10);
        }
    }

    @Autowired
    private GiftPointsService service;
    @Autowired
    private TestEntityManager em;

    private User user;

    @BeforeEach
    void setUp() {
        user = em.persist(new User("Arjun", "arjun@example.com", "hash"));
        user.addGiftPoints(150);
        em.flush();
    }

    private static GiftPointsRedemptionRequest request(int points, String subtotal) {
        return new GiftPointsRedemptionRequest(points, new BigDecimal(subtotal));
    }

    @Test
    void previewShowsDiscountRemainingPointsAndReducedTotal() {
        GiftPointsRedemptionResponse response = service.previewRedemption(user.getId(), request(100, "508.00"));

        assertThat(response.pointsToRedeem()).isEqualTo(100);
        assertThat(response.discountAmount()).isEqualByComparingTo("100.00");
        assertThat(response.remainingPoints()).isEqualTo(50);
        assertThat(response.newOrderTotal()).isEqualByComparingTo("408.00");
    }

    @Test
    void previewDoesNotSpendThePoints() {
        service.previewRedemption(user.getId(), request(100, "508.00"));
        em.flush();
        em.clear();

        assertThat(em.find(User.class, user.getId()).getGiftPoints()).isEqualTo(150);
    }

    @Test
    void redeemingTheWholeBalanceIsAllowed() {
        GiftPointsRedemptionResponse response = service.previewRedemption(user.getId(), request(150, "200.00"));

        assertThat(response.remainingPoints()).isZero();
    }

    @Test
    void redeemingTheWholeSubtotalIsAllowed() {
        GiftPointsRedemptionResponse response = service.previewRedemption(user.getId(), request(120, "120.00"));

        assertThat(response.newOrderTotal()).isEqualByComparingTo("0.00");
    }

    @Test
    void redeemingMoreThanTheBalanceIsRejected() {
        assertThatThrownBy(() -> service.previewRedemption(user.getId(), request(151, "500.00")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("only 150 are available");
    }

    @Test
    void redeemingMoreThanTheSubtotalIsRejected() {
        assertThatThrownBy(() -> service.previewRedemption(user.getId(), request(100, "99.99")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("subtotal");
    }

    @Test
    void unknownUserIsUnauthorized() {
        assertThatThrownBy(() -> service.previewRedemption(9999L, request(1, "10.00")))
                .isInstanceOf(UnauthorizedException.class);
    }
}
