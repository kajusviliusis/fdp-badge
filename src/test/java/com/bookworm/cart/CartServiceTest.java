package com.bookworm.cart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import com.bookworm.book.Book;
import com.bookworm.category.Category;
import com.bookworm.common.error.BusinessRuleException;
import com.bookworm.common.error.ResourceNotFoundException;
import com.bookworm.common.error.UnauthorizedException;
import com.bookworm.pricing.PricingProperties;
import com.bookworm.pricing.PricingService;
import com.bookworm.publisher.Publisher;
import com.bookworm.user.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * Exercises {@link CartService} against a real persistence context to verify the JPA mappings, cascade and
 * orphan-removal behaviour as well as the service logic.
 */
@DataJpaTest
@Import({CartService.class, PricingService.class, CartServiceTest.PricingConfig.class})
class CartServiceTest {

    @TestConfiguration
    static class PricingConfig {
        @Bean
        PricingProperties pricingProperties() {
            return new PricingProperties(new BigDecimal("0.12"), new BigDecimal("500.00"), new BigDecimal("40.00"));
        }
    }

    @Autowired
    private CartService cartService;

    @Autowired
    private TestEntityManager em;

    private User user;
    private Book cheap;
    private Book pricey;

    @BeforeEach
    void setUp() {
        user = em.persist(new User("Arjun", "arjun@example.com", "hash"));
        Category category = em.persist(new Category("Fiction"));
        Publisher publisher = em.persist(new Publisher("Penguin"));
        cheap = em.persist(Book.builder("Cheap", "A", new BigDecimal("149.00"), category, publisher)
                .stockQuantity(10).build());
        pricey = em.persist(Book.builder("Pricey", "B", new BigDecimal("210.00"), category, publisher)
                .stockQuantity(2).build());
        em.flush();
    }

    @Test
    void firstAccessCreatesEmptyCart() {
        CartResponse cart = cartService.getCart(user.getId());

        assertThat(cart.id()).isNotNull();
        assertThat(cart.items()).isEmpty();
        assertThat(cart.subtotal()).isEqualByComparingTo("0");
        assertThat(cart.deliveryCharges()).isEqualByComparingTo("0");
        assertThat(cart.totalItems()).isZero();
    }

    @Test
    void secondAccessReturnsSameCart() {
        Long first = cartService.getCart(user.getId()).id();

        assertThat(cartService.getCart(user.getId()).id()).isEqualTo(first);
    }

    @Test
    void addItemPersistsLineAndComputesTotals() {
        cartService.addItem(user.getId(), new AddCartItemRequest(cheap.getId(), 2));
        CartResponse cart = cartService.addItem(user.getId(), new AddCartItemRequest(pricey.getId(), 1));

        assertThat(cart.items()).hasSize(2);
        assertThat(cart.items().get(0).id()).isNotNull();
        assertThat(cart.items().get(0).lineTotal()).isEqualByComparingTo("298.00");
        assertThat(cart.subtotal()).isEqualByComparingTo("508.00");
        assertThat(cart.tax()).isEqualByComparingTo("60.96");
        assertThat(cart.deliveryCharges()).isEqualByComparingTo("0.00");
        assertThat(cart.totalItems()).isEqualTo(3);
    }

    @Test
    void addItemSurvivesReloadFromDatabase() {
        cartService.addItem(user.getId(), new AddCartItemRequest(cheap.getId(), 2));
        em.flush();
        em.clear();

        CartResponse cart = cartService.getCart(user.getId());

        assertThat(cart.items()).hasSize(1);
        assertThat(cart.items().get(0).book().title()).isEqualTo("Cheap");
        assertThat(cart.items().get(0).book().category().name()).isEqualTo("Fiction");
    }

    @Test
    void addItemForUnknownBookIsNotFound() {
        assertThatThrownBy(() -> cartService.addItem(user.getId(), new AddCartItemRequest(999L, 1)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void addItemBeyondStockIsRejected() {
        assertThatThrownBy(() -> cartService.addItem(user.getId(), new AddCartItemRequest(pricey.getId(), 3)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void updateItemChangesQuantity() {
        cartService.addItem(user.getId(), new AddCartItemRequest(cheap.getId(), 5));

        CartResponse cart = cartService.updateItem(user.getId(), cheap.getId(), new UpdateCartItemRequest(2));

        assertThat(cart.items().get(0).quantity()).isEqualTo(2);
        assertThat(cart.subtotal()).isEqualByComparingTo("298.00");
    }

    @Test
    void removeItemDeletesRowViaOrphanRemoval() {
        cartService.addItem(user.getId(), new AddCartItemRequest(cheap.getId(), 1));
        cartService.addItem(user.getId(), new AddCartItemRequest(pricey.getId(), 1));

        CartResponse cart = cartService.removeItem(user.getId(), cheap.getId());
        em.flush();
        em.clear();

        assertThat(cart.items()).hasSize(1);
        assertThat(cartService.getCart(user.getId()).items()).hasSize(1);
    }

    @Test
    void clearRemovesAllItemsButKeepsCart() {
        Long cartId = cartService.addItem(user.getId(), new AddCartItemRequest(cheap.getId(), 1)).id();

        cartService.clear(user.getId());
        em.flush();
        em.clear();

        CartResponse cart = cartService.getCart(user.getId());
        assertThat(cart.id()).isEqualTo(cartId);
        assertThat(cart.items()).isEmpty();
    }

    @Test
    void cartsAreIsolatedPerUser() {
        User other = em.persist(new User("Other", "other@example.com", "hash"));
        cartService.addItem(user.getId(), new AddCartItemRequest(cheap.getId(), 1));

        assertThat(cartService.getCart(other.getId()).items()).isEmpty();
    }

    @Test
    void unknownUserIsUnauthorized() {
        assertThatThrownBy(() -> cartService.getCart(12345L)).isInstanceOf(UnauthorizedException.class);
    }
}
