package com.bookworm.book;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookRepository extends JpaRepository<Book, Long>, JpaSpecificationExecutor<Book> {

    @Override
    @EntityGraph(attributePaths = "category")
    Page<Book> findAll(Specification<Book> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"category", "publisher"})
    Optional<Book> findWithDetailsById(Long id);

    @EntityGraph(attributePaths = "category")
    List<Book> findWithCategoryByIdIn(Collection<Long> ids);

    /** Returns the first page of all books in the order given by {@code pageable}, without a count query. */
    @EntityGraph(attributePaths = "category")
    List<Book> findBy(Pageable pageable);

    /**
     * Finds books sharing the category or author of the given book, excluding the book itself.
     * Same-author matches rank first, then popularity.
     */
    @EntityGraph(attributePaths = "category")
    @Query("""
            select b from Book b
            where b.id <> :bookId
              and (b.category.id = :categoryId or b.author = :author)
            order by case when b.author = :author then 0 else 1 end, b.copiesSold desc, b.id
            """)
    List<Book> findRelated(@Param("bookId") Long bookId,
                           @Param("categoryId") Long categoryId,
                           @Param("author") String author,
                           Pageable pageable);

    /**
     * In-stock books in any of the given categories or by any of the given authors, other than the excluded
     * ones. Books by a favoured author rank first, then popularity.
     */
    @EntityGraph(attributePaths = "category")
    @Query("""
            select b from Book b
            where b.id not in :excludedIds
              and b.stockQuantity > 0
              and (b.category.id in :categoryIds or b.author in :authors)
            order by case when b.author in :authors then 0 else 1 end, b.copiesSold desc, b.id
            """)
    List<Book> findLikedBy(@Param("excludedIds") Collection<Long> excludedIds,
                           @Param("categoryIds") Collection<Long> categoryIds,
                           @Param("authors") Collection<String> authors,
                           Pageable pageable);

    /** In-stock books other than the excluded ones, most popular first. */
    @EntityGraph(attributePaths = "category")
    @Query("""
            select b from Book b
            where b.id not in :excludedIds and b.stockQuantity > 0
            order by b.copiesSold desc, b.id
            """)
    List<Book> findPopularExcluding(@Param("excludedIds") Collection<Long> excludedIds, Pageable pageable);
}
