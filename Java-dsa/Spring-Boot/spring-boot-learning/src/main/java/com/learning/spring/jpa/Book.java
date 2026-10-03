package com.learning.spring.jpa;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/*
 * The OWNING side of the relationship: the books table has the author_id
 * foreign key column.
 *
 * @ManyToOne is EAGER by default -> we override it with LAZY. Otherwise every
 * "select books" would also load each book's author (another N+1 source).
 */
@Entity
@Table(name = "books")
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private BigDecimal price;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")             // FK column name
    private Author author;

    protected Book() {}

    public Book(String title, BigDecimal price) {
        this.title = title;
        this.price = price;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public BigDecimal getPrice() { return price; }
    public Author getAuthor() { return author; }
    void setAuthor(Author author) { this.author = author; }

    @Override
    public String toString() { return "Book{" + id + ", " + title + "}"; }
}
