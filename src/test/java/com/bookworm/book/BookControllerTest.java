package com.bookworm.book;

import static com.bookworm.support.TestAuth.user;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import com.bookworm.category.CategoryResponse;
import com.bookworm.common.error.ResourceNotFoundException;
import com.bookworm.common.page.PageResponse;
import com.bookworm.publisher.PublisherResponse;
import com.bookworm.support.WebSecurityTestConfig;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BookController.class)
@Import(WebSecurityTestConfig.class)
class BookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookService bookService;

    private static final CategoryResponse CATEGORY = new CategoryResponse(3L, "Fiction");

    private static BookSummaryResponse summary() {
        return new BookSummaryResponse(42L, "Joy of Minimalism", "Daniel Reed", new BigDecimal("149.00"),
                BookFormat.PAPERBACK, null, CATEGORY, 3, true);
    }

    @Test
    void searchBindsQueryParametersAndAppliesDefaults() throws Exception {
        given(bookService.search(any())).willReturn(new PageResponse<>(List.of(summary()), 0, 20, 1, 1));

        mockMvc.perform(get("/books")
                        .param("q", "joy")
                        .param("categoryId", "3")
                        .param("format", "PAPERBACK")
                        .param("minPrice", "10")
                        .param("sortBy", "price_asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Joy of Minimalism"))
                .andExpect(jsonPath("$.content[0].inStock").value(true))
                .andExpect(jsonPath("$.content[0].category.name").value("Fiction"))
                .andExpect(jsonPath("$.totalElements").value(1));

        ArgumentCaptor<BookSearchCriteria> captor = ArgumentCaptor.forClass(BookSearchCriteria.class);
        verify(bookService).search(captor.capture());
        BookSearchCriteria criteria = captor.getValue();
        org.assertj.core.api.Assertions.assertThat(criteria.q()).isEqualTo("joy");
        org.assertj.core.api.Assertions.assertThat(criteria.sortBy()).isEqualTo(BookSortOption.PRICE_ASC);
        org.assertj.core.api.Assertions.assertThat(criteria.page()).isZero();
        org.assertj.core.api.Assertions.assertThat(criteria.size()).isEqualTo(20);
    }

    @Test
    void searchRejectsOutOfRangeSizeWith422() throws Exception {
        mockMvc.perform(get("/books").param("size", "101"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("size"));
    }

    @Test
    void searchRejectsInvertedPriceRangeWith422() throws Exception {
        mockMvc.perform(get("/books").param("minPrice", "50").param("maxPrice", "10"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void searchRejectsUnknownSortOrder() throws Exception {
        mockMvc.perform(get("/books").param("sortBy", "alphabetical"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("sortBy"));
    }

    @Test
    void unsupportedMethodReturns405() throws Exception {
        mockMvc.perform(post("/books").with(user(7)))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void bestsellersUsesDefaultLimitOfTen() throws Exception {
        given(bookService.findBestsellers(10)).willReturn(List.of(summary()));

        mockMvc.perform(get("/books/bestsellers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(42));
    }

    @Test
    void newLaunchesRejectsLimitAboveFifty() throws Exception {
        mockMvc.perform(get("/books/new-launches").param("limit", "51"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("limit"));
    }

    @Test
    void getReturnsBookDetail() throws Exception {
        given(bookService.findById(42L)).willReturn(new BookDetailResponse(42L, "Joy of Minimalism",
                "Daniel Reed", new BigDecimal("149.00"), BookFormat.PAPERBACK, null, CATEGORY, 3, true,
                "Declutter", new PublisherResponse(7L, "ABC Publishers"), "English", "978-3-16-148410-0",
                200, 4.5, 145, 145));

        mockMvc.perform(get("/books/42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publisher.name").value("ABC Publishers"))
                .andExpect(jsonPath("$.stockQuantity").value(200))
                .andExpect(jsonPath("$.averageRating").value(4.5))
                .andExpect(jsonPath("$.coverImageUrl").doesNotExist());
    }

    @Test
    void getReturns404ForUnknownBook() throws Exception {
        given(bookService.findById(99L)).willThrow(ResourceNotFoundException.of("Book", 99L));

        mockMvc.perform(get("/books/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Book with id 99 was not found."))
                .andExpect(jsonPath("$.path").value("/books/99"));
    }

    @Test
    void getReturns400ForNonNumericId() throws Exception {
        mockMvc.perform(get("/books/abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void relatedPassesLimit() throws Exception {
        given(bookService.findRelated(eq(42L), eq(2))).willReturn(List.of(summary()));

        mockMvc.perform(get("/books/42/related").param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Joy of Minimalism"));
    }
}
