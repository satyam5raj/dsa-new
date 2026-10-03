package com.learning.spring.mvc;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/*
 * DTOs (Data Transfer Objects) for the product API.
 *
 * Why separate request/response DTOs instead of exposing entities directly?
 *   - The API contract stays stable even when the DB model changes
 *   - Clients can't set fields they shouldn't (id, createdAt) -> no "mass assignment" bugs
 *   - Validation rules live on the INPUT type
 * Records are perfect DTOs: immutable, concise, and Jackson can (de)serialize them.
 */
public final class ProductModels {

    private ProductModels() {}

    public record Product(long id, String name, BigDecimal price, String category) {}

    /** Request body for POST/PUT. Bean Validation annotations are checked by @Valid. */
    public record ProductRequest(
            @NotBlank(message = "name is required") @Size(max = 100) String name,
            @Positive(message = "price must be greater than 0") BigDecimal price,
            @NotBlank(message = "category is required") String category) {}

    /** Thrown by the service; translated to HTTP 404 by GlobalExceptionHandler. */
    public static class ProductNotFoundException extends RuntimeException {
        public ProductNotFoundException(long id) {
            super("Product " + id + " not found");
        }
    }
}
