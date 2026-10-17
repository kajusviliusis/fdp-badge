package com.bookworm.payment;

import static com.bookworm.support.TestAuth.user;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import com.bookworm.common.error.BusinessRuleException;
import com.bookworm.common.error.ForbiddenException;
import com.bookworm.common.error.ResourceNotFoundException;
import com.bookworm.support.WebSecurityTestConfig;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PaymentController.class)
@Import(WebSecurityTestConfig.class)
class PaymentControllerTest {

    private static final String URL = "/orders/1001/payment";
    private static final String CARD_BODY = """
            {"method":"CREDIT_CARD","cardDetails":{"cardNumber":"4111111111111111","nameOnCard":"Arjun Patel",
            "cvv":"123","expiryDate":"12/2030"}}""";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    private static PaymentResponse payment(PaymentStatus status) {
        return new PaymentResponse(1L, 1001L, PaymentMethod.CREDIT_CARD, status, new BigDecimal("518.96"),
                "TXN-20250301-ABCDE");
    }

    @Test
    void payReturns200WithPayment() throws Exception {
        given(paymentService.pay(any(), any(), any())).willReturn(payment(PaymentStatus.SUCCESS));

        mockMvc.perform(post(URL).with(user(7)).contentType(MediaType.APPLICATION_JSON).content(CARD_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(1001))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.amount").value(518.96))
                .andExpect(jsonPath("$.transactionReference").value("TXN-20250301-ABCDE"));

        verify(paymentService).pay(7L, 1001L, new PaymentRequest(PaymentMethod.CREDIT_CARD,
                new CardDetails("4111111111111111", "Arjun Patel", "123", "12/2030"), null));
    }

    @Test
    void declinedPaymentIsStill200WithFailedStatus() throws Exception {
        given(paymentService.pay(any(), any(), any())).willReturn(payment(PaymentStatus.FAILED));

        mockMvc.perform(post(URL).with(user(7)).contentType(MediaType.APPLICATION_JSON).content(CARD_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"));
    }

    @Test
    void walletNeedsNoDetails() throws Exception {
        given(paymentService.pay(any(), any(), any())).willReturn(payment(PaymentStatus.SUCCESS));

        mockMvc.perform(post(URL).with(user(7)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"method\":\"WALLET\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void methodIsRequired() throws Exception {
        mockMvc.perform(post(URL).with(user(7)).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("method"));

        verifyNoInteractions(paymentService);
    }

    @Test
    void cardMethodRequiresCardDetails() throws Exception {
        mockMvc.perform(post(URL).with(user(7)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"method\":\"DEBIT_CARD\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fieldErrors[0].message").value("cardDetails are required for card payments"));

        verifyNoInteractions(paymentService);
    }

    @Test
    void upiMethodRequiresUpiDetails() throws Exception {
        mockMvc.perform(post(URL).with(user(7)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"method\":\"UPI\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fieldErrors[0].message").value("upiDetails are required for UPI payments"));

        verifyNoInteractions(paymentService);
    }

    @Test
    void invalidCardFieldsAreRejectedWithoutEchoingTheCardData() throws Exception {
        String body = """
                {"method":"CREDIT_CARD","cardDetails":{"cardNumber":"4111-1111","nameOnCard":"Arjun Patel",
                "cvv":"9","expiryDate":"12/2030"}}""";

        mockMvc.perform(post(URL).with(user(7)).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().string(not(containsString("4111-1111"))))
                .andExpect(content().string(not(containsString("\"9\""))));

        verifyNoInteractions(paymentService);
    }

    @Test
    void invalidUpiIdIsRejected() throws Exception {
        mockMvc.perform(post(URL).with(user(7)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"method\":\"UPI\",\"upiDetails\":{\"upiId\":\"nope\"}}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().string(not(containsString("nope"))));
    }

    @Test
    void orderNotAwaitingPaymentMapsTo400() throws Exception {
        given(paymentService.pay(any(), any(), any()))
                .willThrow(new BusinessRuleException("Order is not awaiting payment; its status is CONFIRMED."));

        mockMvc.perform(post(URL).with(user(7)).contentType(MediaType.APPLICATION_JSON).content(CARD_BODY))
                .andExpect(status().isBadRequest());
    }

    @Test
    void anotherUsersOrderMapsTo403() throws Exception {
        given(paymentService.pay(any(), any(), any())).willThrow(new ForbiddenException("Not your order."));

        mockMvc.perform(post(URL).with(user(7)).contentType(MediaType.APPLICATION_JSON).content(CARD_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void payRequiresIdentity() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(CARD_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getReturnsPayment() throws Exception {
        given(paymentService.get(7L, 1001L)).willReturn(payment(PaymentStatus.SUCCESS));

        mockMvc.perform(get(URL).with(user(7)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.method").value("CREDIT_CARD"));
    }

    @Test
    void getWithoutPaymentMapsTo404() throws Exception {
        given(paymentService.get(7L, 1001L)).willThrow(new ResourceNotFoundException("No payment yet."));

        mockMvc.perform(get(URL).with(user(7)))
                .andExpect(status().isNotFound());
    }
}
