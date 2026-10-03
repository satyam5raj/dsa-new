package com.learning.spring.mvc;

import com.learning.spring.mvc.ProductModels.Product;
import com.learning.spring.mvc.ProductModels.ProductNotFoundException;
import com.learning.spring.mvc.ProductModels.ProductRequest;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/*
 * Business layer for products. Kept in-memory so the MVC topic stays focused
 * on the WEB layer (the JPA topic covers persistence).
 *
 * LAYERING: Controller (HTTP: parse, validate, status codes)
 *        -> Service    (business rules, transactions)
 *        -> Repository (data access)
 * Controllers stay thin. They should NOT contain business logic.
 *
 * Singleton bean shared by all request threads -> thread-safe collections.
 */
@Service
public class ProductService {

    private final Map<Long, Product> products = new ConcurrentHashMap<>();
    private final AtomicLong ids = new AtomicLong();

    public ProductService() {
        create(new ProductRequest("Mechanical Keyboard", new java.math.BigDecimal("4999.00"), "electronics"));
        create(new ProductRequest("Clean Code", new java.math.BigDecimal("650.00"), "books"));
    }

    public List<Product> findAll(String category) {
        return products.values().stream()
                .filter(p -> category == null || p.category().equalsIgnoreCase(category))
                .sorted(Comparator.comparingLong(Product::id))
                .toList();
    }

    public Product findById(long id) {
        Product p = products.get(id);
        if (p == null) throw new ProductNotFoundException(id);
        return p;
    }

    public Product create(ProductRequest req) {
        long id = ids.incrementAndGet();
        Product p = new Product(id, req.name(), req.price(), req.category());
        products.put(id, p);
        return p;
    }

    public Product update(long id, ProductRequest req) {
        findById(id);                                             // 404 if missing
        Product updated = new Product(id, req.name(), req.price(), req.category());
        products.put(id, updated);
        return updated;
    }

    public void delete(long id) {
        if (products.remove(id) == null) throw new ProductNotFoundException(id);
    }
}
