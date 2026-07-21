package org.example.hive.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Date;

import static com.auth0.jwt.algorithms.Algorithm.HMAC256;

@Service
public class JwtService {

    private static final String CLAIM_COMPANY_ID = "companyId";

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.issuer}")
    private String issuer;

    @Value("${app.jwt.access-expiration-minutes}")
    private long accessExpirationMinutes;

    public String generateToken(UserDetails userDetails) {
        long expirationMillis = accessExpirationMinutes * 60 * 1000;

        var builder = JWT.create()
                .withSubject(userDetails.getUsername())
                .withIssuer(issuer)
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + expirationMillis));

        if (userDetails instanceof AuthUserPrincipal principal && principal.getCompanyId() != null) {
            builder.withClaim(CLAIM_COMPANY_ID, principal.getCompanyId());
        }

        return builder.sign(HMAC256(jwtSecret));
    }

    public String extractUsername(String token) {
        return decode(token).getSubject();
    }

    public Long extractCompanyId(String token) {
        DecodedJWT jwt = decode(token);
        if (!jwt.getClaims().containsKey(CLAIM_COMPANY_ID)) {
            return null;
        }
        return jwt.getClaim(CLAIM_COMPANY_ID).asLong();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return decode(token).getExpiresAt().before(new Date());
    }

    private DecodedJWT decode(String token) {
        return JWT.require(HMAC256(jwtSecret))
                .withIssuer(issuer)
                .build()
                .verify(token);
    }
}
