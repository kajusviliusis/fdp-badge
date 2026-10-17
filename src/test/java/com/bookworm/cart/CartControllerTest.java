package com.bookworm.cart;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import com.bookworm.book.BookFormat;
import com.bookworm.book.BookSummaryResponse;
import com.bookworm.category.CategoryResponse;
import com.bookworm.common.error.BusinessRuleException;
import com.bookworm.common.error.ResourceNotFoundException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CartController.class)
class CartControllerTest {

    private static final String USER_HEADER = "X-User-Id";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CartService cartService;

    private static CartResponse cart() {
        BookSummaryResponse book = new BookSummaryResponse(42L, "Joy of Minimalism", "Daniel Reed",
                new BigDecimal("149.00"), BookFormat.PAPERBACK, null, new CategoryResponse(3L, "Fiction"), 3, true);
        return new CartResponse(1L, List.of(new CartItemResponse(10L, book, 2, new BigDecimal("298.00"))),
                new BigDecimal("298.00"), new BigDecimal("35.76"), new BigDecimal("40.00"), 2);
    }

    @Test
    void getReturnsCartForCurrentUser() throws Exception {
        given(cartService.getCart(7L)).willReturn(cart());

        mockMvc.perform(get("/cart").header(USER_HEADER, "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].book.title").value("Joy of Minimalism"))
                .andExpect(jsonPath("$.items[0].lineTotal").value(298.00))
                .andExpect(jsonPath("$.subtotal").value(298.00))
                .andExpect(jsonPath("$.deliveryCharges").value(40.00))
                .andExpect(jsonPath("$.totalItems").value(2));
    }

    @Test
    void missingIdentityIs401() throws Exception {
        mockMvc.perform(get("/cart"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void malformedIdentityIs401() throws Exception {
        mockMvc.perform(get("/cart").header(USER_HEADER, "abc"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void addItemReturnsUpdatedCartWith200() throws Exception {
        given(cartService.addItem(eq(7L), any())).willReturn(cart());

        mockMvc.perform(post("/cart/items").header(USER_HEADER, "7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookId\":42,\"quantity\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(cartService).addItem(7L, new AddCartItemRequest(42L, 2));
    }

    @Test
    void addItemRejectsQuantityOutOfRange() throws Exception {
        mockMvc.perform(post("/cart/items").header(USER_HEADER, "7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookId\":42,\"quantity\":100}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("quantity"));
    }

    @Test
    void addItemRejectsMissingBookId() throws Exception {
        mockMvc.perform(post("/cart/items").header(USER_HEADER, "7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":1}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("bookId"));
    }

    @Test
    void addItemMapsBusinessRuleViolationTo400() throws Exception {
        given(cartService.addItem(eq(7L), any())).willThrow(new BusinessRuleException("Only 1 copies in stock."));

        mockMvc.perform(post("/cart/items").header(USER_HEADER, "7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookId\":42,\"quantity\":5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Only 1 copies in stock."));
    }

    @Test
    void addItemMapsUnknownBookTo404() throws Exception {
        given(cartService.addItem(eq(7L), any())).willThrow(ResourceNotFoundException.of("Book", 42L));

        mockMvc.perform(post("/cart/items").header(USER_HEADER, "7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookId\":42,\"quantity\":1}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateItemPassesBookIdAndQuantity() throws Exception {
        given(cartService.updateItem(eq(7L), eq(42L), any())).willReturn(cart());

        mockMvc.perform(put("/cart/items/42").header(USER_HEADER, "7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":3}"))
                .andExpect(status().isOk());

        verify(cartService).updateItem(7L, 42L, new UpdateCartItemRequest(3));
    }

    @Test
    void updateItemRejectsZeroQuantity() throws Exception {
        mockMvc.perform(put("/cart/items/42").header(USER_HEADER, "7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":0}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void removeItemReturnsUpdatedCart() throws Exception {
        given(cartService.removeItem(7L, 42L)).willReturn(cart());

        mockMvc.perform(delete("/cart/items/42").header(USER_HEADER, "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void removeItemNotInCartIs404() throws Exception {
        given(cartService.removeItem(7L, 42L)).willThrow(new ResourceNotFoundException("not in cart"));

        mockMvc.perform(delete("/cart/items/42").header(USER_HEADER, "7"))
                .andExpect(status().isNotFound());
    }

    @Test
    void clearReturns204() throws Exception {
        mockMvc.perform(delete("/cart/clear").header(USER_HEADER, "7"))
                .andExpect(status().isNoContent());

        verify(cartService).clear(7L);
    }
}
