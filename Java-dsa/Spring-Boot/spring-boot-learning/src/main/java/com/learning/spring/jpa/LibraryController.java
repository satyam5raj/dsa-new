package com.learning.spring.jpa;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/*
 * Run with the dev profile to SEE the SQL in the console:
 *   ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
 *   curl 'localhost:8080/api/library/book-counts?strategy=n-plus-one'   -> 1 + N selects in the log
 *   curl 'localhost:8080/api/library/book-counts?strategy=join-fetch'   -> a single select with a join
 */
@RestController
public class LibraryController {

    private final LibraryService library;

    public LibraryController(LibraryService library) {
        this.library = library;
    }

    @GetMapping("/api/library/book-counts")
    public Object bookCounts(@RequestParam(defaultValue = "join-fetch") String strategy) {
        return switch (strategy) {
            case "n-plus-one" -> library.bookCountsNPlusOne();
            case "entity-graph" -> library.bookCountsEntityGraph();
            case "projection" -> library.bookCountsProjection();
            default -> library.bookCountsJoinFetch();
        };
    }

    /*
     * Sample data for running the app. A CommandLineRunner runs once after startup.
     * @Profile("!test") keeps it out of the integration tests, which create
     * their own data.
     */
    @Configuration
    @Profile("!test")
    static class SampleData {
        @Bean
        CommandLineRunner seedLibrary(AuthorRepository authors) {
            return args -> {
                if (authors.count() > 0) return;
                authors.save(new Author("Joshua Bloch")
                        .addBook(new Book("Effective Java", new BigDecimal("3200")))
                        .addBook(new Book("Java Puzzlers", new BigDecimal("2100"))));
                authors.save(new Author("Robert Martin")
                        .addBook(new Book("Clean Code", new BigDecimal("650")))
                        .addBook(new Book("Clean Architecture", new BigDecimal("700"))));
                authors.save(new Author("Martin Kleppmann")
                        .addBook(new Book("Designing Data-Intensive Applications", new BigDecimal("1500"))));
            };
        }
    }
}
