package com.bookworm.book;

import java.util.List;

import com.bookworm.common.page.PageResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/books")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping
    public PageResponse<BookSummaryResponse> search(@Valid @ModelAttribute BookSearchCriteria criteria) {
        return bookService.search(criteria);
    }

    @GetMapping("/bestsellers")
    public List<BookSummaryResponse> bestsellers(
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit) {
        return bookService.findBestsellers(limit);
    }

    @GetMapping("/new-launches")
    public List<BookSummaryResponse> newLaunches(
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit) {
        return bookService.findNewLaunches(limit);
    }

    @GetMapping("/{bookId}")
    public BookDetailResponse get(@PathVariable Long bookId) {
        return bookService.findById(bookId);
    }

    @GetMapping("/{bookId}/related")
    public List<BookSummaryResponse> related(
            @PathVariable Long bookId,
            @RequestParam(defaultValue = "5") @Min(1) @Max(20) int limit) {
        return bookService.findRelated(bookId, limit);
    }
}
