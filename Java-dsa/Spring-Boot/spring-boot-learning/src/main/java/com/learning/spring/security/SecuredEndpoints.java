package com.learning.spring.security;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/*
 * TWO LEVELS OF AUTHORIZATION
 *   1. URL-based (SecurityConfig): /api/admin/** needs ROLE_ADMIN. Coarse-grained, checked in the filter chain.
 *   2. METHOD-based (@PreAuthorize): fine-grained, checked by an AOP proxy around the
 *      bean method. It can use method arguments:
 *        @PreAuthorize("hasRole('ADMIN') or #username == authentication.name")
 *      Lives in the SERVICE layer, so it protects the logic no matter which
 *      controller (or message listener) calls it.
 */
@RestController
public class SecuredEndpoints {

    private final ReportService reports;

    public SecuredEndpoints(ReportService reports) {
        this.reports = reports;
    }

    // Any authenticated user. @AuthenticationPrincipal injects the validated JWT.
    // Besides ROLE_USER you'll see "FACTOR_BEARER": Spring Security 7 records HOW the user
    // authenticated (a bearer token here; FACTOR_PASSWORD for a password login). That's the
    // building block for multi-factor rules like "this endpoint needs password AND OTT".
    @GetMapping("/api/me")
    public Map<String, Object> me(@AuthenticationPrincipal Jwt jwt, Authentication auth) {
        return Map.of(
                "username", jwt.getSubject(),
                "authorities", auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList(),
                "tokenExpiresAt", String.valueOf(jwt.getExpiresAt()));
    }

    // URL rule: ADMIN only
    @GetMapping("/api/admin/stats")
    public Map<String, Object> adminStats() {
        return Map.of("activeUsers", 42, "ordersToday", 1337);
    }

    // URL: any authenticated user. METHOD security decides inside the service.
    @GetMapping("/api/reports/sales")
    public Map<String, Object> salesReport() {
        return reports.salesReport();
    }

    @GetMapping("/api/users/{username}/profile")
    public Map<String, Object> profile(@PathVariable String username) {
        return reports.profileOf(username);
    }

    @Service
    public static class ReportService {

        @PreAuthorize("hasRole('ADMIN')")
        public Map<String, Object> salesReport() {
            return Map.of("revenue", 1_250_000, "currency", "INR");
        }

        // Users can read THEIR OWN profile; admins can read anyone's
        @PreAuthorize("hasRole('ADMIN') or #username == authentication.name")
        public Map<String, Object> profileOf(String username) {
            return Map.of("username", username, "plan", "premium");
        }
    }
}
