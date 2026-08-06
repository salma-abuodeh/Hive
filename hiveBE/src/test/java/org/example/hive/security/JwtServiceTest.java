package org.example.hive.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "jwtSecret", "test-secret-key-for-unit-tests-1234567890");
        ReflectionTestUtils.setField(jwtService, "issuer", "HiveTest");
        ReflectionTestUtils.setField(jwtService, "accessExpirationMinutes", 60L);
    }

    @Test
    @DisplayName("Should generate valid JWT token with companyId claim and extract values back")
    void tokenGenerationAndExtraction() {
        AuthUserPrincipal principal = new AuthUserPrincipal(
                100L,
                42L,
                "user@hive.local",
                "secret",
                true,
                Collections.emptyList()
        );

        String token = jwtService.generateToken(principal);

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractUsername(token)).isEqualTo("user@hive.local");
        assertThat(jwtService.extractCompanyId(token)).isEqualTo(42L);
        assertThat(jwtService.isTokenValid(token, principal)).isTrue();
    }
}