package org.example.hive.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Date;

import static com.auth0.jwt.algorithms.Algorithm.HMAC256;

@Service
public class JwtService {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.issuer}")
    private String issuer;

    @Value("${app.jwt.access-expiration-minutes}")
    private long accessExpirationMinutes;

    public String generateToken(UserDetails userDetails) {
        long expirationMillis = accessExpirationMinutes * 60 * 1000;

        return JWT.create()
                .withSubject(userDetails.getUsername())
                .withIssuer(issuer)
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + expirationMillis))
                .sign(HMAC256(jwtSecret));
    }

    public String extractUsername(String token) {
        return JWT.require(HMAC256(jwtSecret))
                .withIssuer(issuer)
                .build()
                .verify(token)
                .getSubject();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        Date expiresAt = JWT.require(HMAC256(jwtSecret))
                .withIssuer(issuer)
                .build()
                .verify(token)
                .getExpiresAt();

        return expiresAt.before(new Date());
    }
}
