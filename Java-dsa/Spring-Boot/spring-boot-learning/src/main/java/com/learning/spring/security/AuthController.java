package com.learning.spring.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/*
 * Issues JWTs:
 *   curl -X POST localhost:8080/auth/token -H 'Content-Type: application/json' \
 *        -d '{"username":"admin","password":"admin123"}'
 *
 * A JWT has 3 base64url parts:  HEADER.PAYLOAD.SIGNATURE
 *   header  : {"alg":"HS256"}
 *   payload : {"sub":"admin","roles":["USER","ADMIN"],"iss":"...","iat":...,"exp":...}  <- NOT encrypted!
 *   signature: HMAC(header.payload, secret) -> proves nobody tampered with it
 * Anyone can READ the payload (paste one into jwt.io) -> never put secrets in it.
 * Keep access tokens short-lived. Use refresh tokens for long sessions.
 */
@RestController
public class AuthController {

    public record TokenRequest(String username, String password) {}
    public record TokenResponse(String token, long expiresInSeconds, List<String> roles) {}

    private final AuthenticationManager authManager;
    private final JwtEncoder encoder;
    private final Clock clock;
    private final String issuer;
    private final Duration ttl;

    public AuthController(AuthenticationManager authManager, JwtEncoder encoder, Clock clock,
                          @Value("${app.jwt.issuer}") String issuer, @Value("${app.jwt.ttl}") Duration ttl) {
        this.authManager = authManager;
        this.encoder = encoder;
        this.clock = clock;
        this.issuer = issuer;
        this.ttl = ttl;
    }

    @PostMapping("/auth/token")
    public TokenResponse token(@RequestBody TokenRequest request) {
        // Throws BadCredentialsException (-> 401 via GlobalExceptionHandler) if the password is wrong
        Authentication auth = authManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.password()));

        List<String> roles = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring("ROLE_".length()))
                .toList();

        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(auth.getName())
                .issuedAt(now)
                .expiresAt(now.plus(ttl))
                .claim("roles", roles)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new TokenResponse(token, ttl.toSeconds(), roles);
    }
}
