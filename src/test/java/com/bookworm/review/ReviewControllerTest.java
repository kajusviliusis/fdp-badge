package com.bookworm.review;

import static com.bookworm.support.TestAuth.user;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import com.bookworm.common.error.ConflictException;
import com.bookworm.common.error.ForbiddenException;
import com.bookworm.common.error.ResourceNotFoundException;
import com.bookworm.common.page.PageResponse;
import com.bookworm.support.WebSecurityTestConfig;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ReviewController.class)
@Import(WebSecurityTestConfig.class)
class ReviewControllerTest {

    private static final String URL = "/books/42/reviews";
    private static final String VALID_BODY = "{\"rating\":5,\"comment\":\"A refreshing take on clarity.\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReviewService reviewService;

    private static ReviewResponse review() {
        return new ReviewResponse(1L, 42L, 7L, "John Smith", 5, "A refreshing take on clarity.",
                Instant.parse("2025-03-01T12:00:00Z"));
    }

    @Test
    void listIsPublicAndUsesDefaultPaging() throws Exception {
        given(reviewService.list(42L, 0, 10)).willReturn(new PageResponse<>(List.of(review()), 0, 10, 1, 1));

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].reviewerName").value("John Smith"))
                .andExpect(jsonPath("$.content[0].rating").value(5))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void listRejectsOutOfRangeSize() throws Exception {
        mockMvc.perform(get(URL).param("size", "51"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("size"));
    }

    @Test
    void listOfUnknownBookIs404() throws Exception {
        given(reviewService.list(42L, 0, 10)).willThrow(ResourceNotFoundException.of("Book", 42L));

        mockMvc.perform(get(URL)).andExpect(status().isNotFound());
    }

    @Test
    void createReturns201() throws Exception {
        given(reviewService.create(any(), any(), any())).willReturn(review());

        mockMvc.perform(post(URL).with(user(7)).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.bookId").value(42));

        verify(reviewService).create(7L, 42L, new ReviewRequest(5, "A refreshing take on clarity."));
    }

    @Test
    void createRequiresAuthentication() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(reviewService);
    }

    @Test
    void createRejectsRatingOutsideOneToFive() throws Exception {
        mockMvc.perform(post(URL).with(user(7)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":6,\"comment\":\"A refreshing take on clarity.\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("rating"));
        mockMvc.perform(post(URL).with(user(7)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":0,\"comment\":\"A refreshing take on clarity.\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void createRejectsShortAndMissingComment() throws Exception {
        mockMvc.perform(post(URL).with(user(7)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":5,\"comment\":\"too short\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("comment"));
        mockMvc.perform(post(URL).with(user(7)).contentType(MediaType.APPLICATION_JSON).content("{\"rating\":5}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void createRejectsMissingRating() throws Exception {
        mockMvc.perform(post(URL).with(user(7)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"A refreshing take on clarity.\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("rating"));
    }

    @Test
    void createMapsNonBuyerTo403() throws Exception {
        given(reviewService.create(any(), any(), any())).willThrow(new ForbiddenException("Not a buyer."));

        mockMvc.perform(post(URL).with(user(7)).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void createMapsDuplicateTo409() throws Exception {
        given(reviewService.create(any(), any(), any())).willThrow(new ConflictException("Already reviewed."));

        mockMvc.perform(post(URL).with(user(7)).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isConflict());
    }

    @Test
    void createMapsUnknownBookTo404() throws Exception {
        given(reviewService.create(any(), any(), any())).willThrow(ResourceNotFoundException.of("Book", 42L));

        mockMvc.perform(post(URL).with(user(7)).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isNotFound());
    }
}
