package com.learning.spring.mvc;

import com.learning.spring.mvc.ProductModels.Product;
import com.learning.spring.mvc.ProductModels.ProductNotFoundException;
import com.learning.spring.mvc.ProductModels.ProductRequest;
import com.learning.spring.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * ============================================================================
 *  TOPIC 03 + 09 : SLICE TEST for the web layer with @WebMvcTest + MockMvc
 * ============================================================================
 *
 *  @WebMvcTest(ProductController.class) starts ONLY the MVC infrastructure:
 *  this controller, @ControllerAdvice, JSON converters, validation, filters
 *  (incl. Spring Security). NO services, repositories or database -> fast and focused.
 *  Dependencies of the controller are replaced with mocks via @MockitoBean
 *  (which replaced the deprecated @MockBean in Spring Framework 6.2+).
 *
 *  MockMvc performs requests through the DispatcherServlet WITHOUT a real
 *  HTTP server, then asserts on status, headers and JSON (jsonPath).
 *  @Import(SecurityConfig.class): our real security rules apply in the slice too.
 */
@WebMvcTest(ProductController.class)
@Import(SecurityConfig.class)
class ProductControllerTest {

    @Autowired MockMvc mvc;
    @MockitoBean ProductService service;      // a Mockito mock registered as a bean

    @Test
    void listProducts_returnsJsonArray() throws Exception {
        given(service.findAll(null)).willReturn(List.of(
                new Product(1, "Keyboard", new BigDecimal("4999.00"), "electronics"),
                new Product(2, "Clean Code", new BigDecimal("650.00"), "books")));

        mvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name").value("Keyboard"))
                .andExpect(jsonPath("$[1].price").value(650.00));
    }

    @Test
    void getMissingProduct_returns404ProblemDetail() throws Exception {
        given(service.findById(99)).willThrow(new ProductNotFoundException(99));

        mvc.perform(get("/api/products/99"))
                .andDo(print())                                         // prints the full request/response
                .andExpect(status().isNotFound())
                .andExpect(header().string("Content-Type", "application/problem+json"))
                .andExpect(jsonPath("$.title").value("Product not found"))
                .andExpect(jsonPath("$.detail").value("Product 99 not found"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void createValidProduct_returns201WithLocation() throws Exception {
        given(service.create(any(ProductRequest.class)))
                .willReturn(new Product(7, "Mouse", new BigDecimal("799"), "electronics"));

        mvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Mouse","price":799,"category":"electronics"}"""))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/products/7")))
                .andExpect(jsonPath("$.id").value(7));
    }

    @Test
    void createInvalidProduct_returns400WithAllFieldErrors_andNeverCallsService() throws Exception {
        mvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","price":-5}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").value("name is required"))
                .andExpect(jsonPath("$.errors.price").value("price must be greater than 0"))
                .andExpect(jsonPath("$.errors.category").value("category is required"));

        then(service).should(never()).create(any());     // validation stopped it before the service
    }

    @Test
    void malformedJson_returns400() throws Exception {
        mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest());
    }
}
