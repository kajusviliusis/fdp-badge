package com.bookworm.recommendation;

import static com.bookworm.support.TestAuth.user;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import com.bookworm.book.BookFormat;
import com.bookworm.book.BookSummaryResponse;
import com.bookworm.category.CategoryResponse;
import com.bookworm.support.WebSecurityTestConfig;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RecommendationController.class)
@Import(WebSecurityTestConfig.class)
class RecommendationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecommendationService recommendationService;

    private static BookSummaryResponse book() {
        return new BookSummaryResponse(42L, "Joy of Minimalism", "Daniel Reed", new BigDecimal("149.00"),
                BookFormat.PAPERBACK, null, new CategoryResponse(3L, "Fiction"), 3, true);
    }

    @Test
    void returnsRecommendationsForTheCurrentUserWithDefaultLimit() throws Exception {
        given(recommendationService.recommendFor(7L, 10)).willReturn(List.of(book()));

        mockMvc.perform(get("/recommendations").with(user(7)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Joy of Minimalism"))
                .andExpect(jsonPath("$[0].inStock").value(true));
    }

    @Test
    void limitIsPassedThrough() throws Exception {
        given(recommendationService.recommendFor(7L, 3)).willReturn(List.of());

        mockMvc.perform(get("/recommendations").with(user(7)).param("limit", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void limitOutOfRangeIs422() throws Exception {
        mockMvc.perform(get("/recommendations").with(user(7)).param("limit", "51"))
                .andExpect(status().isUnprocessableEntity());
        mockMvc.perform(get("/recommendations").with(user(7)).param("limit", "0"))
                .andExpect(status().isUnprocessableEntity());

        verifyNoInteractions(recommendationService);
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/recommendations")).andExpect(status().isUnauthorized());

        verifyNoInteractions(recommendationService);
    }
}
