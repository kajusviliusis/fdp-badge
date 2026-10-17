package com.bookworm.giftpoints;

import static com.bookworm.support.TestAuth.user;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import com.bookworm.common.error.BusinessRuleException;
import com.bookworm.support.WebSecurityTestConfig;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(GiftPointsController.class)
@Import(WebSecurityTestConfig.class)
class GiftPointsControllerTest {

    private static final String URL = "/recommendations/gift-points/redeem";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GiftPointsService giftPointsService;

    @Test
    void previewReturnsDiscountAndRemainingPoints() throws Exception {
        given(giftPointsService.previewRedemption(any(), any())).willReturn(new GiftPointsRedemptionResponse(
                100, new BigDecimal("100.00"), 50, new BigDecimal("408.00")));

        mockMvc.perform(post(URL).with(user(7)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pointsToRedeem\":100,\"orderSubtotal\":508.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pointsToRedeem").value(100))
                .andExpect(jsonPath("$.discountAmount").value(100.00))
                .andExpect(jsonPath("$.remainingPoints").value(50))
                .andExpect(jsonPath("$.newOrderTotal").value(408.00));

        verify(giftPointsService).previewRedemption(7L,
                new GiftPointsRedemptionRequest(100, new BigDecimal("508.00")));
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pointsToRedeem\":100,\"orderSubtotal\":508.00}"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(giftPointsService);
    }

    @Test
    void pointsMustBeAtLeastOne() throws Exception {
        mockMvc.perform(post(URL).with(user(7)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pointsToRedeem\":0,\"orderSubtotal\":508.00}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("pointsToRedeem"));
    }

    @Test
    void subtotalMustNotBeNegative() throws Exception {
        mockMvc.perform(post(URL).with(user(7)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pointsToRedeem\":10,\"orderSubtotal\":-1}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("orderSubtotal"));
    }

    @Test
    void bothFieldsAreRequired() throws Exception {
        mockMvc.perform(post(URL).with(user(7)).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void businessRuleViolationIs400() throws Exception {
        given(giftPointsService.previewRedemption(any(), any()))
                .willThrow(new BusinessRuleException("Cannot redeem 500 gift points; only 150 are available."));

        mockMvc.perform(post(URL).with(user(7)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pointsToRedeem\":500,\"orderSubtotal\":900.00}"))
                .andExpect(status().isBadRequest());
    }
}
