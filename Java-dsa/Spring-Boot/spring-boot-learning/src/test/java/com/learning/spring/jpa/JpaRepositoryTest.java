package com.learning.spring.jpa;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.LazyInitializationException;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/*
 * ============================================================================
 *  TOPIC 04 + 09 : @DataJpaTest - repositories, query methods, N+1, lazy loading
 * ============================================================================
 *
 *  @DataJpaTest starts ONLY the JPA slice: entities, repositories, an embedded DB,
 *  Hibernate. No web layer, no services (we @Import the one we need).
 *  By default EVERY TEST runs in a transaction that is ROLLED BACK at the end.
 *  Here we switch that off (NOT_SUPPORTED) on purpose: lazy loading and N+1 only
 *  show their real behaviour when transactions begin and end like in production.
 *
 *  Hibernate STATISTICS count the SQL statements, so the N+1 problem becomes a
 *  number we can assert on.
 */
@DataJpaTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@Import(LibraryService.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class JpaRepositoryTest {

    @Autowired AuthorRepository authors;
    @Autowired BookRepository books;
    @Autowired LibraryService library;
    @Autowired EntityManagerFactory emf;
    @Autowired TransactionTemplate tx;

    Statistics stats;

    @BeforeEach
    void seed() {
        stats = emf.unwrap(SessionFactory.class).getStatistics();
        for (int i = 1; i <= 5; i++) {
            Author a = new Author("Author " + i);
            for (int j = 1; j <= 3; j++) a.addBook(new Book("Book " + i + "." + j, BigDecimal.valueOf(100L * i + j)));
            authors.save(a);                                   // cascade = ALL saves the books too
        }
    }

    @AfterEach
    void cleanUp() {
        authors.deleteAll();                                   // orphanRemoval/cascade deletes books
    }

    // ---------------------------------------------------------------
    // Query methods
    // ---------------------------------------------------------------
    @Test
    void derivedQueries() {
        assertThat(authors.findByName("Author 2")).isPresent();
        assertThat(authors.findByNameContainingIgnoreCaseOrderByNameAsc("AUTHOR")).hasSize(5);
        assertThat(books.findByAuthorName("Author 1")).hasSize(3);                       // join through the relationship
        assertThat(books.countByAuthorName("Author 3")).isEqualTo(3);
        assertThat(books.findTop3ByOrderByPriceDesc()).extracting(Book::getTitle)
                .containsExactly("Book 5.3", "Book 5.2", "Book 5.1");
        assertThat(books.findByPriceLessThan(new BigDecimal("103"), Sort.by("title"))).extracting(Book::getTitle)
                .containsExactly("Book 1.1", "Book 1.2");
    }

    @Test
    void paginationAndProjections() {
        Page<Book> page = books.findByTitleContainingIgnoreCase("book", PageRequest.of(1, 4, Sort.by("title")));
        assertThat(page.getTotalElements()).isEqualTo(15);
        assertThat(page.getTotalPages()).isEqualTo(4);
        assertThat(page.getContent()).extracting(Book::getTitle)
                .containsExactly("Book 2.2", "Book 2.3", "Book 3.1", "Book 3.2");      // 2nd page of 4

        List<BookRepository.TitleAndPrice> projected = books.findByAuthorNameOrderByTitle("Author 1");
        assertThat(projected).extracting(BookRepository.TitleAndPrice::getTitle).containsExactly("Book 1.1", "Book 1.2", "Book 1.3");
    }

    @Test
    void bulkUpdateWithModifyingQuery() {
        int updated = tx.execute(s -> books.applyPriceFactor("Author 1", new BigDecimal("2")));
        assertThat(updated).isEqualTo(3);
        assertThat(books.findByAuthorName("Author 1")).extracting(b -> b.getPrice().intValue())
                .containsExactlyInAnyOrder(202, 204, 206);
    }

    // ---------------------------------------------------------------
    // The N+1 problem, measured
    // ---------------------------------------------------------------
    @Test
    void nPlusOne_vs_fixes() {
        long nPlusOne = countStatements(library::bookCountsNPlusOne);
        long joinFetch = countStatements(library::bookCountsJoinFetch);
        long entityGraph = countStatements(library::bookCountsEntityGraph);
        long projection = countStatements(library::bookCountsProjection);

        System.out.printf("   SQL statements -> N+1: %d | JOIN FETCH: %d | @EntityGraph: %d | DTO projection: %d%n",
                nPlusOne, joinFetch, entityGraph, projection);
        assertThat(nPlusOne).isEqualTo(1 + 5);      // 1 for authors + 1 per author
        assertThat(joinFetch).isEqualTo(1);
        assertThat(entityGraph).isEqualTo(1);
        assertThat(projection).isEqualTo(1);
        assertThat(library.bookCountsJoinFetch()).containsEntry("Author 1", 3);
    }

    // ---------------------------------------------------------------
    // LazyInitializationException: touching a LAZY collection after the
    // transaction (and its session) has closed
    // ---------------------------------------------------------------
    @Test
    void lazyLoadingOutsideTransaction_fails() {
        Author detached = authors.findByName("Author 1").orElseThrow();   // tx opened and CLOSED inside findByName
        assertThatThrownBy(() -> detached.getBooks().size())
                .isInstanceOf(LazyInitializationException.class);

        // Inside a transaction the same access works
        Integer count = tx.execute(s -> authors.findByName("Author 1").orElseThrow().getBooks().size());
        assertThat(count).isEqualTo(3);
    }

    private long countStatements(Supplier<?> action) {
        stats.clear();
        action.get();
        return stats.getPrepareStatementCount();
    }
}
