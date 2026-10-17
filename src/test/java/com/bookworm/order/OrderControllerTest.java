package com.bookworm.order;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.bookworm.address.AddressResponse;
import com.bookworm.cart.CartResponse;
import com.bookworm.common.error.BusinessRuleException;
import com.bookworm.common.error.ForbiddenException;
import com.bookworm.common.error.ResourceNotFoundException;
import com.bookworm.common.page.PageResponse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    private static final String USER = "X-User-Id";
    private static final Instant CREATED = Instant.parse("2025-03-01T12:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CheckoutService checkoutService;

    @MockitoBean
    private OrderService orderService;

    private static OrderResponse order() {
        AddressResponse address = new AddressResponse(5L, "Arjun", "Patel", "12 MG Road", "Mumbai", null,
                "400001", "India", "+919876543210", null);
        return new OrderResponse(1001L, OrderStatus.PENDING_PAYMENT, List.of(), address,
                new BigDecimal("508.00"), new BigDecimal("60.96"), new BigDecimal("0.00"),
                new BigDecimal("50.00"), new BigDecimal("518.96"), 50, 51, CREATED, CREATED.plusSeconds(172800),
                null);
    }

    @Test
    void createReturns201WithOrder() throws Exception {
        given(checkoutService.placeOrder(any(), any())).willReturn(order());

        mockMvc.perform(post("/orders").header(USER, "7").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"addressId\":5,\"giftPointsToRedeem\":50}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1001))
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.totalAmount").value(518.96))
                .andExpect(jsonPath("$.cancellationDeadline").value("2025-03-03T12:00:00Z"));

        verify(checkoutService).placeOrder(7L, new CreateOrderRequest(5L, 50, null));
    }

    @Test
    void createRequiresAddressId() throws Exception {
        mockMvc.perform(post("/orders").header(USER, "7").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("addressId"));
    }

    @Test
    void createRejectsNegativeGiftPoints() throws Exception {
        mockMvc.perform(post("/orders").header(USER, "7").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"addressId\":5,\"giftPointsToRedeem\":-1}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("giftPointsToRedeem"));
    }

    @Test
    void createMapsBusinessRuleTo400() throws Exception {
        given(checkoutService.placeOrder(any(), any())).willThrow(new BusinessRuleException("Cannot place an order with an empty cart."));

        mockMvc.perform(post("/orders").header(USER, "7").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"addressId\":5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot place an order with an empty cart."));
    }

    @Test
    void createMapsUnknownAddressTo404() throws Exception {
        given(checkoutService.placeOrder(any(), any())).willThrow(ResourceNotFoundException.of("Address", 5L));

        mockMvc.perform(post("/orders").header(USER, "7").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"addressId\":5}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void requiresIdentity() throws Exception {
        mockMvc.perform(get("/orders")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/orders/1/cancel")).andExpect(status().isUnauthorized());
    }

    @Test
    void listUsesDefaultsAndStatusFilter() throws Exception {
        given(orderService.list(7L, OrderStatus.CANCELLED, 0, 10))
                .willReturn(new PageResponse<>(List.of(order()), 0, 10, 1, 1));

        mockMvc.perform(get("/orders").header(USER, "7").param("status", "CANCELLED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1001))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void listRejectsSizeAboveFifty() throws Exception {
        mockMvc.perform(get("/orders").header(USER, "7").param("size", "51"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("size"));
    }

    @Test
    void listRejectsUnknownStatus() throws Exception {
        mockMvc.perform(get("/orders").header(USER, "7").param("status", "LOST"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getReturnsOrder() throws Exception {
        given(orderService.get(7L, 1001L)).willReturn(order());

        mockMvc.perform(get("/orders/1001").header(USER, "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.address.city").value("Mumbai"));
    }

    @Test
    void getMapsForeignOrderTo403() throws Exception {
        given(orderService.get(7L, 1001L)).willThrow(new ForbiddenException("You do not have access to this order."));

        mockMvc.perform(get("/orders/1001").header(USER, "7"))
                .andExpect(status().isForbidden());
    }

    @Test
    void cancelReturnsOrder() throws Exception {
        given(orderService.cancel(7L, 1001L)).willReturn(order());

        mockMvc.perform(post("/orders/1001/cancel").header(USER, "7"))
                .andExpect(status().isOk());
    }

    @Test
    void cancelOutsideWindowMapsTo400() throws Exception {
        given(orderService.cancel(7L, 1001L)).willThrow(new BusinessRuleException("The 48-hour cancellation window for this order has expired."));

        mockMvc.perform(post("/orders/1001/cancel").header(USER, "7"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void buyAgainReturnsCart() throws Exception {
        given(orderService.buyAgain(7L, 1001L))
                .willReturn(new CartResponse(1L, List.of(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0));

        mockMvc.perform(post("/orders/1001/buy-again").header(USER, "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }
}
