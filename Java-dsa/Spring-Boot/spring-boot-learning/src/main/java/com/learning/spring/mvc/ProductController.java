package com.learning.spring.mvc;

import com.learning.spring.mvc.ProductModels.Product;
import com.learning.spring.mvc.ProductModels.ProductRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/*
 * ============================================================================
 *  TOPIC 03 : SPRING MVC - REST CONTROLLERS & REQUEST MAPPING
 * ============================================================================
 *
 *  HOW A REQUEST FLOWS THROUGH SPRING MVC
 *    Client -> Tomcat -> Filter chain (Spring Security lives here!)
 *           -> DispatcherServlet (the "front controller": ONE servlet for everything)
 *              1. HandlerMapping   : which controller method matches the URL + HTTP method?
 *              2. HandlerAdapter   : call it, resolving arguments
 *                                    (@PathVariable, @RequestParam, @RequestBody -> JSON via
 *                                     HttpMessageConverter/Jackson, @Valid -> validation)
 *              3. Return value     -> HttpMessageConverter writes JSON
 *              4. Exception?       -> HandlerExceptionResolver -> @ControllerAdvice
 *
 *  @RestController = @Controller + @ResponseBody (return values are written to the
 *                    response body as JSON, instead of being treated as view names)
 *
 *  REST CONVENTIONS used here
 *    GET    /api/products            list (200)         ?category=books filters
 *    GET    /api/products/{id}       one (200 / 404)
 *    POST   /api/products            create (201 + Location header / 400)
 *    PUT    /api/products/{id}       replace (200 / 400 / 404)
 *    DELETE /api/products/{id}       delete (204 / 404)
 *
 *  Try (app running):
 *    curl localhost:8080/api/products
 *    curl -i -X POST localhost:8080/api/products -H 'Content-Type: application/json' \
 *         -d '{"name":"Mouse","price":799,"category":"electronics"}'
 *    curl -i -X POST localhost:8080/api/products -H 'Content-Type: application/json' -d '{"price":-1}'
 */
@RestController
@RequestMapping("/api/products")                 // common prefix for every method below
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    // GET /api/products?category=books   (query parameter, optional)
    @GetMapping
    public List<Product> list(@RequestParam(required = false) String category) {
        return service.findAll(category);
    }

    // GET /api/products/42   (path variable). 404 is handled by GlobalExceptionHandler.
    @GetMapping("/{id}")
    public Product get(@PathVariable long id,
                       @RequestHeader(value = "X-Request-Id", required = false) String requestId) {  // reading a header
        return service.findById(id);
    }

    // POST with a JSON body. @Valid triggers Bean Validation -> 400 on failure.
    // ResponseEntity gives full control: status code + headers + body.
    @PostMapping
    public ResponseEntity<Product> create(@Valid @RequestBody ProductRequest request) {
        Product created = service.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);      // 201 + Location: /api/products/3
    }

    @PutMapping("/{id}")
    public Product update(@PathVariable long id, @Valid @RequestBody ProductRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)                       // 204, no body
    public void delete(@PathVariable long id) {
        service.delete(id);
    }
}
