package com.bookworm.book;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * Lets query parameters such as {@code sortBy=price_asc} bind to {@link BookSortOption}.
 */
@Component
public class BookSortOptionConverter implements Converter<String, BookSortOption> {

    @Override
    public BookSortOption convert(String source) {
        return source.isBlank() ? null : BookSortOption.fromValue(source.strip());
    }
}
