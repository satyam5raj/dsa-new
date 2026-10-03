package com.learning.spring.mvc;

import com.learning.spring.mvc.ProductModels.ProductNotFoundException;
import com.learning.spring.tx.BankService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

/*
 * ============================================================================
 *  TOPIC 03 : CENTRALIZED EXCEPTION HANDLING with @RestControllerAdvice
 * ============================================================================
 *
 *  Without this, every controller would need try/catch blocks, and clients
 *  would get inconsistent error formats (or stack traces!).
 *
 *  @RestControllerAdvice = a global interceptor for exceptions thrown by ANY controller.
 *  @ExceptionHandler(X.class) = "when X escapes a controller, call me instead".
 *  The most SPECIFIC matching handler wins.
 *
 *  ProblemDetail (RFC 9457 "Problem Details for HTTP APIs") is the standard
 *  error body that Spring supports natively:
 *      { "type": "...", "title": "Not Found", "status": 404,
 *        "detail": "Product 99 not found", "instance": "/api/products/99" }
 *  Content-Type: application/problem+json
 *
 *  NOTE: exceptions thrown by Spring Security FILTERS (missing token -> 401,
 *  wrong role on a URL -> 403) happen BEFORE the DispatcherServlet, so they
 *  never reach this class. They're handled by the security filter chain's
 *  AuthenticationEntryPoint / AccessDeniedHandler.
 *  We deliberately do NOT add a catch-all @ExceptionHandler(Exception.class):
 *  it would also swallow AccessDeniedException from @PreAuthorize and turn 403s into 500s.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 404 - resource doesn't exist
    @ExceptionHandler(ProductNotFoundException.class)
    public ProblemDetail handleNotFound(ProductNotFoundException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle("Product not found");
        pd.setType(URI.create("https://learning.dev/errors/not-found"));
        return pd;
    }

    // 400 - @Valid failed: report EVERY invalid field, not just the first
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fe -> errors.put(fe.getField(), fe.getDefaultMessage()));
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
        pd.setTitle("Invalid request");
        pd.setProperty("errors", errors);            // custom extension field
        return pd;
    }

    // 401 - wrong username/password on POST /auth/token
    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuth(AuthenticationException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Invalid username or password");
    }

    // 422 - a business rule was violated (used by the transactions topic's API)
    @ExceptionHandler(BankService.BankingException.class)
    public ProblemDetail handleBanking(BankService.BankingException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage());
    }
}
