package com.learning.spring.jpa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

/*
 * QUERY METHOD CHEAT SHEET (derived from the method name)
 *   findByTitle(String)                      where title = ?
 *   findByPriceLessThan(BigDecimal, Sort)    where price < ?  order by ...
 *   findByPriceBetween(a, b)                 where price between ? and ?
 *   findByTitleContainingIgnoreCase(s)       where upper(title) like upper('%s%')
 *   findByAuthorName(String)                 where author.name = ?   (walks the relationship -> join)
 *   findTop3ByOrderByPriceDesc()             order by price desc limit 3
 *   countByAuthorName / existsByTitle        count / exists queries
 *   findByTitleIn(Collection)                where title in (...)
 * Return types: List, Optional, Stream, Page (with total count), Slice (no count),
 * or a projection interface/record.
 *
 * When the name gets too long -> @Query with JPQL (entity names, not table names),
 * or nativeQuery = true for raw SQL.
 */
public interface BookRepository extends JpaRepository<Book, Long> {

    List<Book> findByPriceLessThan(BigDecimal max, Sort sort);

    List<Book> findByAuthorName(String authorName);

    List<Book> findTop3ByOrderByPriceDesc();

    long countByAuthorName(String authorName);

    // PAGINATION: Pageable carries page number, size and sort.
    // Page<T> also runs a COUNT query to know the total number of pages.
    Page<Book> findByTitleContainingIgnoreCase(String fragment, Pageable pageable);

    // INTERFACE PROJECTION: Spring selects only the needed columns
    interface TitleAndPrice {
        String getTitle();
        BigDecimal getPrice();
    }

    List<TitleAndPrice> findByAuthorNameOrderByTitle(String authorName);

    // Bulk UPDATE in one statement. @Modifying is required for update/delete queries.
    // It bypasses the persistence context: managed entities may now be STALE
    // -> clearAutomatically = true evicts them.
    @Modifying(clearAutomatically = true)
    @Query("update Book b set b.price = b.price * :factor where b.author.name = :author")
    int applyPriceFactor(@Param("author") String author, @Param("factor") BigDecimal factor);
}
