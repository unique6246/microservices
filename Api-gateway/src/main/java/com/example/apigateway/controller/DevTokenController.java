package com.example.apigateway.controller;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * DEV-ONLY endpoint to generate signed JWT tokens for local testing.
 *
 * Usage:
 *   POST /auth/token
 *   Content-Type: application/json
 *   {
 *     "username": "alice",
 *     "roles": ["USER", "ADMIN"]   // optional, defaults to ["USER"]
 *   }
 *
 * Use the returned token in subsequent requests:
 *   Authorization: Bearer <token>
 *
 * This controller is NOT active in prod — annotated with @Profile("dev").
 */
@Slf4j
@RestController
@Profile("dev")
@RequestMapping("/auth")
public class DevTokenController {

    @Value("${app.jwt.secret}")
    private String secret;

    @PostMapping("/token")
    public Mono<Map<String, Object>> generateToken(@RequestBody TokenRequest request) {
        if (request.username() == null || request.username().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "username is required");
        }

        try {
            MACSigner signer = new MACSigner(secret.getBytes(StandardCharsets.UTF_8));

            List<String> roles = (request.roles() != null && !request.roles().isEmpty())
                    ? request.roles()
                    : List.of("USER");

            long nowMs  = System.currentTimeMillis();
            long expMs  = nowMs + 3_600_000L;   // 1 hour

            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(request.username())
                    .issuer("dev-local-gateway")
                    .issueTime(new Date(nowMs))
                    .expirationTime(new Date(expMs))
                    .claim("roles", roles)
                    .claim("username", request.username())
                    .build();

            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            jwt.sign(signer);

            String token = jwt.serialize();
            log.info("🔑 [DEV] Issued token for user='{}' roles={}", request.username(), roles);

            return Mono.just(Map.of(
                    "access_token", token,
                    "token_type",   "Bearer",
                    "expires_in",   3600,
                    "username",     request.username(),
                    "roles",        roles
            ));

        } catch (Exception ex) {
            log.error("Failed to generate dev token", ex);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Token generation failed: " + ex.getMessage());
        }
    }

    public record TokenRequest(String username, List<String> roles) {}
}

