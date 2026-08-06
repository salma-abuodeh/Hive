package org.example.hive.security;

import org.example.hive.model.Company;
import org.example.hive.model.User;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
public class JwtTestHelper {

    private final JwtService jwtService;

    public JwtTestHelper(JwtService jwtService) {
        this.jwtService = jwtService;
    }


    public String generate(User user, Company company) {

        AuthUserPrincipal principal = new AuthUserPrincipal(
                user.getId(),
                company.getId(),
                user.getEmail(),
                user.getPassword(),
                true,
                Collections.emptyList()
        );

        return jwtService.generateToken(principal);
    }


    public String bearer(User user, Company company) {
        return "Bearer " + generate(user, company);
    }
}