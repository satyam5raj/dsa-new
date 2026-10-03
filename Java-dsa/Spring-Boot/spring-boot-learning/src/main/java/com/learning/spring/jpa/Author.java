package com.learning.spring.jpa;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

/*
 * ============================================================================
 *  TOPIC 04 : JPA ENTITY MAPPING  (Author 1 --- * Book)
 * ============================================================================
 *
 *  JPA = the specification (jakarta.persistence.*). Hibernate = the implementation.
 *  Spring Data JPA = repositories on top of JPA (no boilerplate DAO code).
 *
 *  @Entity   : this class maps to a table (here "authors")
 *  @Id       : primary key. @GeneratedValue(IDENTITY) = auto-increment column.
 *  Entities need a no-arg constructor (protected is fine): Hibernate creates
 *  instances by reflection and may subclass them for lazy-loading PROXIES.
 *
 *  RELATIONSHIPS
 *   @OneToMany(mappedBy = "author") : the INVERSE side. The foreign key column
 *       lives in the books table, owned by Book.author (the OWNING side).
 *       Only changes to the OWNING side are written to the DB, so always keep
 *       both sides in sync with a helper like addBook().
 *   cascade = ALL       : persist/merge/remove an author -> same for their books
 *   orphanRemoval = true: remove a book from the list -> DELETE it from the DB
 *
 *  FETCH TYPES (defaults matter, memorize them!)
 *   @OneToMany, @ManyToMany  -> LAZY by default (good)
 *   @ManyToOne, @OneToOne    -> EAGER by default (usually BAD: set LAZY explicitly)
 *   LAZY  = load the association only when first accessed (a proxy/collection
 *           wrapper loads it on demand). Needs an open session/transaction at that moment!
 *   EAGER = always load it immediately. Easy, but loads data you don't need, and
 *           can't be turned off per query.
 *   BEST PRACTICE: map everything LAZY, then fetch what each use case needs
 *   with JOIN FETCH or an @EntityGraph (see AuthorRepository).
 */
@Entity
@Table(name = "authors")
public class Author {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL, orphanRemoval = true)   // LAZY by default
    private List<Book> books = new ArrayList<>();

    protected Author() {}                       // for JPA

    public Author(String name) {
        this.name = name;
    }

    // Keeps BOTH sides of the relationship consistent
    public Author addBook(Book book) {
        books.add(book);
        book.setAuthor(this);
        return this;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public List<Book> getBooks() { return books; }

    // equals/hashCode for entities: base them on the id (or a natural key), never on
    // lazy collections. Left as identity here for simplicity.
    @Override
    public String toString() { return "Author{" + id + ", " + name + "}"; }   // never print lazy collections in toString!
}
