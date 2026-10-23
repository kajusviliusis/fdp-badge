package com.bookworm.book;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import com.bookworm.category.Category;
import com.bookworm.publisher.Publisher;
import com.bookworm.support.PostgresDataJpaTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@PostgresDataJpaTest
class BookRepositoryTest {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private TestEntityManager em;

    private Category fiction;
    private Category science;
    private Publisher penguin;
    private Publisher abc;

    @BeforeEach
    void setUp() {
        fiction = em.persist(new Category("Fiction"));
        science = em.persist(new Category("Science"));
        penguin = em.persist(new Publisher("Penguin"));
        abc = em.persist(new Publisher("ABC Publishers"));
    }

    @Test
    void searchMatchesTitleOrAuthorCaseInsensitively() {
        persist("Joy of Minimalism", "Daniel Reed", "100.00", fiction, penguin, 5, 10);
        persist("Cosmos", "Carl Sagan", "200.00", science, abc, 5, 20);

        assertThat(titles(search(criteria("MINIMALISM", null, null, null, null, null, null)))).containsExactly("Joy of Minimalism");
        assertThat(titles(search(criteria("sagan", null, null, null, null, null, null)))).containsExactly("Cosmos");
    }

    @Test
    void searchMatchesStemmedWordsAndRequiresAllTerms() {
        persist("Cooking for Beginners", "Anna Cook", "100.00", fiction, penguin, 5, 0);
        persist("Cooking with Fire", "Mark Flame", "100.00", fiction, penguin, 5, 0);

        assertThat(titles(search(criteria("cooks", null, null, null, null, null, null))))
                .containsExactlyInAnyOrder("Cooking for Beginners", "Cooking with Fire");
        assertThat(titles(search(criteria("cooking fire", null, null, null, null, null, null))))
                .containsExactly("Cooking with Fire");
    }

    @Test
    void searchSupportsWebSearchSyntaxAndNeverFailsOnArbitraryInput() {
        persist("Pure Maths", "Someone", "50.00", fiction, penguin, 1, 0);
        persist("Applied Maths", "Someone Else", "50.00", fiction, penguin, 1, 0);

        assertThat(titles(search(criteria("maths -applied", null, null, null, null, null, null)))).containsExactly("Pure Maths");
        assertThat(titles(search(criteria("\"pure maths\"", null, null, null, null, null, null)))).containsExactly("Pure Maths");
        assertThat(search(criteria("%_'(&|!:*", null, null, null, null, null, null)).getTotalElements()).isZero();
    }

    @Test
    void relevanceSortRanksBetterTextMatchesFirstThenPopularity() {
        persist("Space", "Sagan Space", "10.00", science, abc, 1, 1);
        persist("Space Opera", "Someone", "10.00", fiction, penguin, 1, 500);
        persist("Space Space Space Odyssey", "Someone", "10.00", fiction, penguin, 1, 2);

        Page<Book> results = bookRepository.findAll(
                BookSpecifications.matching(criteria("space", null, null, null, null, null, null)),
                PageRequest.of(0, 10));

        assertThat(titles(results)).containsExactly("Space Space Space Odyssey", "Space", "Space Opera");
    }

    @Test
    void relevanceSortWithoutTextFallsBackToPopularity() {
        persist("Quiet", "A", "10.00", fiction, penguin, 1, 1);
        persist("Loud", "B", "10.00", fiction, penguin, 1, 9);

        BookSearchCriteria noText = criteria(null, null, null, null, null, null, null);

        assertThat(noText.rankedByText()).isFalse();
        assertThat(titles(search(noText))).containsExactly("Loud", "Quiet");
    }

    @Test
    void textSearchCombinesWithFiltersAndPaginates() {
        persist("Java Basics", "Ann", "100.00", science, abc, 1, 3);
        persist("Java Advanced", "Ann", "300.00", science, abc, 1, 2);
        persist("Java Cookbook", "Ann", "200.00", fiction, penguin, 1, 1);

        Page<Book> page = bookRepository.findAll(
                BookSpecifications.matching(criteria("java", science.getId(), null, null, null, null, null)),
                PageRequest.of(0, 1));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(titles(page)).containsExactly("Java Basics");
    }

    @Test
    void searchFiltersByCategoryPublisherFormatLanguageAndPrice() {
        persist("A", "X", "100.00", fiction, penguin, 1, 0);
        persist("B", "Y", "200.00", science, abc, 1, 0);
        Book hindi = Book.builder("C", "Z", new BigDecimal("300.00"), science, abc)
                .format(BookFormat.EBOOK).language("Hindi").stockQuantity(1).build();
        em.persist(hindi);
        em.flush();

        assertThat(titles(search(criteria(null, science.getId(), null, null, null, null, null)))).containsExactlyInAnyOrder("B", "C");
        assertThat(titles(search(criteria(null, null, penguin.getId(), null, null, null, null)))).containsExactly("A");
        assertThat(titles(search(criteria(null, null, null, "hindi", null, null, null)))).containsExactly("C");
        assertThat(titles(search(criteria(null, null, null, null, BookFormat.EBOOK, null, null)))).containsExactly("C");
        assertThat(titles(search(criteria(null, null, null, null, null, "150", "250")))).containsExactly("B");
    }

    @Test
    void searchSortsAndPaginates() {
        persist("Cheap", "A", "10.00", fiction, penguin, 1, 1);
        persist("Mid", "B", "20.00", fiction, penguin, 1, 3);
        persist("Pricey", "C", "30.00", fiction, penguin, 1, 2);

        Page<Book> byPriceDesc = bookRepository.findAll(
                BookSpecifications.matching(criteria(null, null, null, null, null, null, null)),
                PageRequest.of(0, 2, BookSortOption.PRICE_DESC.toSort()));
        assertThat(titles(byPriceDesc)).containsExactly("Pricey", "Mid");
        assertThat(byPriceDesc.getTotalElements()).isEqualTo(3);
        assertThat(byPriceDesc.getTotalPages()).isEqualTo(2);

        Page<Book> byBestseller = bookRepository.findAll(
                BookSpecifications.matching(criteria(null, null, null, null, null, null, null)),
                PageRequest.of(0, 3, BookSortOption.BESTSELLER.toSort()));
        assertThat(titles(byBestseller)).containsExactly("Mid", "Pricey", "Cheap");
    }

    @Test
    void findByReturnsTopBooksInRequestedOrder() {
        persist("Old", "A", "10.00", fiction, penguin, 1, 1);
        persist("Hot", "B", "10.00", fiction, penguin, 1, 99);

        List<Book> bestsellers = bookRepository.findBy(PageRequest.of(0, 1, BookSortOption.BESTSELLER.toSort()));
        assertThat(bestsellers).extracting(Book::getTitle).containsExactly("Hot");
        assertThat(bestsellers.get(0).getCategory().getName()).isEqualTo("Fiction");

        List<Book> newest = bookRepository.findBy(PageRequest.of(0, 1, BookSortOption.NEWEST.toSort()));
        assertThat(newest).hasSize(1);
    }

    @Test
    void findRelatedExcludesSelfAndRanksSameAuthorFirst() {
        Book anchor = persist("Anchor", "Author One", "10.00", fiction, penguin, 1, 0);
        persist("Same Category", "Someone", "10.00", fiction, penguin, 1, 500);
        persist("Same Author", "Author One", "10.00", science, abc, 1, 1);
        persist("Unrelated", "Other", "10.00", science, abc, 1, 1000);

        List<Book> related = bookRepository.findRelated(
                anchor.getId(), fiction.getId(), anchor.getAuthor(), PageRequest.of(0, 5));

        assertThat(related).extracting(Book::getTitle).containsExactly("Same Author", "Same Category");
    }

    @Test
    void findWithDetailsByIdLoadsCategoryAndPublisher() {
        Book book = persist("Detail", "A", "10.00", fiction, penguin, 0, 0);
        em.clear();

        Book loaded = bookRepository.findWithDetailsById(book.getId()).orElseThrow();

        assertThat(loaded.getCategory().getName()).isEqualTo("Fiction");
        assertThat(loaded.getPublisher().getName()).isEqualTo("Penguin");
        assertThat(loaded.isInStock()).isFalse();
    }

    private Book persist(String title, String author, String price, Category category, Publisher publisher,
                         int stock, int copiesSold) {
        Book book = em.persist(Book.builder(title, author, new BigDecimal(price), category, publisher)
                .stockQuantity(stock).copiesSold(copiesSold).build());
        em.flush();
        return book;
    }

    private Page<Book> search(BookSearchCriteria criteria) {
        return bookRepository.findAll(BookSpecifications.matching(criteria),
                PageRequest.of(criteria.page(), criteria.size(),
                        criteria.rankedByText() ? Sort.unsorted() : criteria.sortBy().toSort()));
    }

    private static BookSearchCriteria criteria(String q, Long categoryId, Long publisherId, String language,
                                               BookFormat format, String minPrice, String maxPrice) {
        return new BookSearchCriteria(q, categoryId, publisherId, language, format,
                minPrice == null ? null : new BigDecimal(minPrice),
                maxPrice == null ? null : new BigDecimal(maxPrice),
                null, null, null);
    }

    private static List<String> titles(Page<Book> page) {
        return page.getContent().stream().map(Book::getTitle).toList();
    }
}
