package com.learning.spring.jpa;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/*
 * ============================================================================
 *  TOPIC 04 : SPRING DATA REPOSITORIES & FIXING N+1
 * ============================================================================
 *
 *  You write an INTERFACE; Spring Data generates the implementation at startup.
 *  JpaRepository<Author, Long> already gives you:
 *    save, saveAll, findById, findAll (+ Sort, + Pageable), count, existsById,
 *    deleteById, delete, flush ...
 *
 *  THE N+1 SELECT PROBLEM
 *    List<Author> authors = repo.findAll();          // 1 query:  select * from authors
 *    for (Author a : authors) a.getBooks().size();   // N queries: select * from books where author_id=?
 *    -> 1 + N round-trips. With 1,000 authors that's 1,001 queries for one page.
 *    It hides in innocent code (loops, JSON serialization, toString) and only
 *    shows up under real data volumes.
 *
 *  FIXES
 *    1. JOIN FETCH in JPQL     -> one query with a JOIN; collections filled immediately
 *    2. @EntityGraph           -> same effect, declared on a repository method
 *    3. @BatchSize / hibernate.default_batch_fetch_size=50
 *                              -> lazy loads N collections in batches: "where author_id in (?, ?, ...)"
 *    4. DTO projections        -> select exactly the columns you need (often best for read APIs)
 *  CAVEAT: JOIN FETCH on a collection + pagination makes Hibernate paginate IN
 *  MEMORY (it warns HHH90003004). For paged lists, page the parent ids first,
 *  or use batch fetching.
 */
public interface AuthorRepository extends JpaRepository<Author, Long> {

    // Derived query: Spring parses the METHOD NAME into JPQL
    //   -> select a from Author a where a.name = ?1
    Optional<Author> findByName(String name);

    List<Author> findByNameContainingIgnoreCaseOrderByNameAsc(String fragment);

    // FIX 1: JOIN FETCH. "distinct" avoids duplicate parents from the join
    // (Hibernate 6+ de-duplicates automatically, but it documents intent).
    @Query("select distinct a from Author a left join fetch a.books")
    List<Author> findAllWithBooks();

    // FIX 2: an entity graph tells JPA which associations to load eagerly for THIS query only
    @EntityGraph(attributePaths = "books")
    @Query("select a from Author a")
    List<Author> findAllWithBooksGraph();

    // FIX 4: DTO projection. Aggregate in the DB; no entities, no lazy loading at all.
    @Query("select new com.learning.spring.jpa.AuthorRepository$AuthorBookCount(a.name, count(b)) " +
           "from Author a left join a.books b group by a.name order by a.name")
    List<AuthorBookCount> countBooksPerAuthor();

    record AuthorBookCount(String author, long books) {}
}
