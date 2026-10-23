package com.bookworm.book;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import com.bookworm.category.Category;
import com.bookworm.publisher.Publisher;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;
    @Mock
    private SalesReport salesReport;

    private BookService serviceAt(String instant) {
        return new BookService(bookRepository, salesReport, Clock.fixed(Instant.parse(instant), ZoneOffset.UTC));
    }

    @Test
    void bestsellersCoverTheCurrentCalendarMonthInUtc() {
        given(salesReport.topSellingBookIds(any(), any(), org.mockito.ArgumentMatchers.anyInt())).willReturn(List.of());

        serviceAt("2025-02-15T10:30:00Z").findBestsellers(5);

        verify(salesReport).topSellingBookIds(
                Instant.parse("2025-02-01T00:00:00Z"), Instant.parse("2025-03-01T00:00:00Z"), 5);
    }

    @Test
    void bestsellersWindowRollsOverTheYearBoundary() {
        given(salesReport.topSellingBookIds(any(), any(), org.mockito.ArgumentMatchers.anyInt())).willReturn(List.of());

        serviceAt("2025-12-31T23:59:59Z").findBestsellers(10);

        verify(salesReport).topSellingBookIds(
                Instant.parse("2025-12-01T00:00:00Z"), Instant.parse("2026-01-01T00:00:00Z"), 10);
    }

    @Test
    void bestsellersKeepTheSalesRankingOrder() {
        Book first = book(7L, "First");
        Book second = book(3L, "Second");
        given(salesReport.topSellingBookIds(any(), any(), org.mockito.ArgumentMatchers.anyInt()))
                .willReturn(List.of(7L, 3L));
        given(bookRepository.findWithCategoryByIdIn(List.of(7L, 3L))).willReturn(List.of(second, first));

        List<BookSummaryResponse> result = serviceAt("2025-02-15T10:30:00Z").findBestsellers(10);

        assertThat(result).extracting(BookSummaryResponse::title).containsExactly("First", "Second");
    }

    @Test
    void bestsellersAreEmptyWhenNothingSoldThisMonth() {
        given(salesReport.topSellingBookIds(any(), any(), org.mockito.ArgumentMatchers.anyInt())).willReturn(List.of());

        assertThat(serviceAt("2025-02-15T10:30:00Z").findBestsellers(10)).isEmpty();
        verifyNoInteractions(bookRepository);
    }

    private static Book book(Long id, String title) {
        Book book = Book.builder(title, "Author", new BigDecimal("100.00"), new Category("Fiction"),
                new Publisher("Penguin")).stockQuantity(1).build();
        ReflectionTestUtils.setField(book, "id", id);
        return book;
    }
}
