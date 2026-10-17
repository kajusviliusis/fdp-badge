package com.bookworm.common.page;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

/**
 * Stable JSON representation of a page of results, decoupled from Spring Data's {@link Page} serialisation.
 */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <S, T> PageResponse<T> of(Page<S> source, Function<? super S, ? extends T> mapper) {
        List<T> content = source.getContent().stream().<T>map(mapper).toList();
        return new PageResponse<>(content, source.getNumber(), source.getSize(),
                source.getTotalElements(), source.getTotalPages());
    }
}
