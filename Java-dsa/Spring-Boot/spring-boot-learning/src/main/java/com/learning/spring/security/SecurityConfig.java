package com.learning.spring.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

/*
 * ============================================================================
 *  TOPIC 06 : SPRING SECURITY - THE FILTER CHAIN, AUTHENTICATION, AUTHORIZATION, JWT
 * ============================================================================
 *
 *  AUTHENTICATION = WHO are you?        (username/password, JWT, OAuth2 login...)  -> 401 if it fails
 *  AUTHORIZATION  = WHAT may you do?    (roles/authorities checked on URLs/methods) -> 403 if it fails
 *
 *  ARCHITECTURE (maps to Express/Nest middleware if you know Node):
 *    HTTP request
 *      -> Servlet container filters
 *         -> DelegatingFilterProxy -> FilterChainProxy -> SecurityFilterChain
 *              (an ORDERED list of filters; the important ones:)
 *              CorsFilter, CsrfFilter, ...
 *              BearerTokenAuthenticationFilter   <- reads "Authorization: Bearer <jwt>",
 *                                                   validates it with JwtDecoder,
 *                                                   puts an Authentication into the
 *                                                   SecurityContext (thread-local, per request)
 *              ExceptionTranslationFilter        <- turns auth exceptions into 401/403
 *              AuthorizationFilter               <- checks the URL rules below
 *      -> DispatcherServlet -> controller (method security @PreAuthorize via AOP proxy)
 *
 *  JWT FLOW IN THIS APP (stateless, like most REST APIs / microservices)
 *    1. POST /auth/token {username,password} -> AuthenticationManager checks the
 *       password (BCrypt) -> we SIGN a JWT (HMAC-SHA256) with the user's roles
 *    2. Client sends  Authorization: Bearer <token>  on every request
 *    3. The resource-server filter VERIFIES the signature + expiry, with no DB or
 *       session lookup. The roles come from the token's "roles" claim.
 *    In real systems an Identity Provider (Keycloak, Auth0, Cognito) usually issues
 *    the tokens (RS256 with public keys / JWKS), and services only VALIDATE them:
 *       spring.security.oauth2.resourceserver.jwt.issuer-uri=https://idp/...
 *
 *  WHY CSRF IS DISABLED: CSRF attacks abuse COOKIES that browsers attach
 *  automatically. A stateless API that only accepts a Bearer token in a header
 *  isn't vulnerable. KEEP CSRF ON for cookie/session-based browser apps!
 */
@Configuration
@EnableMethodSecurity            // enables @PreAuthorize / @PostAuthorize on methods
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))  // no HttpSession
            .headers(h -> h.frameOptions(f -> f.sameOrigin()))      // lets the H2 console's frames render
            .authorizeHttpRequests(auth -> auth
                // rules are checked TOP-DOWN, the first match wins -> specific before general
                .requestMatchers("/auth/**").permitAll()
                .requestMatchers("/api/products/**", "/api/config", "/api/scopes",
                                 "/api/library/**", "/api/bank/**", "/api/orders/**").permitAll()
                .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")   // checks authority "ROLE_ADMIN"
                .requestMatchers("/actuator/**").hasRole("ADMIN")    // metrics, beans, env... are sensitive
                .anyRequest().authenticated())                        // DENY-BY-DEFAULT for everything else
            .oauth2ResourceServer(rs -> rs.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
            .httpBasic(Customizer.withDefaults());                   // also accept Basic auth (handy with curl -u)
        return http.build();
    }

    // Map the JWT "roles" claim (["USER","ADMIN"]) to authorities ROLE_USER, ROLE_ADMIN
    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName("roles");
        roles.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(roles);
        return converter;
    }

    // ---------------- users & passwords ----------------

    // DelegatingPasswordEncoder: stores "{bcrypt}$2a$10$..." so the algorithm can be
    // upgraded later. NEVER store plain-text or fast hashes (MD5/SHA) for passwords.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    // Demo users in memory. Real apps implement UserDetailsService over a users table.
    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder encoder) {
        return new InMemoryUserDetailsManager(
                User.withUsername("user").password(encoder.encode("password")).roles("USER").build(),
                User.withUsername("admin").password(encoder.encode("admin123")).roles("USER", "ADMIN").build());
    }

    // Checks username + password (used by AuthController to issue tokens)
    @Bean
    public AuthenticationManager authenticationManager(UserDetailsService users, PasswordEncoder encoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(users);
        provider.setPasswordEncoder(encoder);
        return new ProviderManager(provider);
    }

    // ---------------- JWT signing / verification (HMAC-SHA256 shared secret) ----------------

    @Bean
    public SecretKey jwtSecretKey(@Value("${app.jwt.secret}") String base64Secret) {
        return new SecretKeySpec(Base64.getDecoder().decode(base64Secret), "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey key) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(key));
    }

    @Bean
    public JwtDecoder jwtDecoder(SecretKey key) {
        return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    }
}
