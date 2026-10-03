package com.learning.spring.jpa;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/*
 * The same use case ("how many books does each author have?") written four ways.
 * JpaRepositoryTest counts the SQL statements each one executes.
 *
 * @Transactional(readOnly = true) on the class:
 *   - keeps ONE persistence context (session) open for the whole method, so lazy
 *     loading works inside it
 *   - readOnly lets Hibernate skip dirty checking (faster), and some drivers
 *     route to read replicas
 */
@Service
@Transactional(readOnly = true)
public class LibraryService {

    private final AuthorRepository authors;

    public LibraryService(AuthorRepository authors) {
        this.authors = authors;
    }

    // 1 + N queries: findAll() loads authors, then EACH getBooks().size() triggers a query
    public Map<String, Integer> bookCountsNPlusOne() {
        Map<String, Integer> result = new LinkedHashMap<>();
        for (Author a : authors.findAll()) {
            result.put(a.getName(), a.getBooks().size());      // lazy load -> SELECT ... WHERE author_id = ?
        }
        return result;
    }

    // 1 query (JOIN FETCH)
    public Map<String, Integer> bookCountsJoinFetch() {
        return toCounts(authors.findAllWithBooks());
    }

    // 1 query (@EntityGraph)
    public Map<String, Integer> bookCountsEntityGraph() {
        return toCounts(authors.findAllWithBooksGraph());
    }

    // 1 query, no entities at all (aggregation done by the database)
    public List<AuthorRepository.AuthorBookCount> bookCountsProjection() {
        return authors.countBooksPerAuthor();
    }

    private Map<String, Integer> toCounts(List<Author> list) {
        Map<String, Integer> result = new LinkedHashMap<>();
        for (Author a : list) result.put(a.getName(), a.getBooks().size());   // already loaded: no query
        return result;
    }
}
