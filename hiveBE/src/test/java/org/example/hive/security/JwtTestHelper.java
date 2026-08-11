package org.example.hive.security;

import org.example.hive.model.Company;
import org.example.hive.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class JwtTestHelper {

    private final JwtService jwtService;

    public JwtTestHelper(JwtService jwtService) {
        this.jwtService = jwtService;
    }


    public String generate(User user, Company company) {
        return generate(user, company, Collections.emptyList());
    }

    public String generate(User user, Company company, List<String> permissions) {

        List<GrantedAuthority> authorities = permissions.stream()
                .<GrantedAuthority>map(SimpleGrantedAuthority::new)
                .toList();

        AuthUserPrincipal principal = new AuthUserPrincipal(
                user.getId(),
                company.getId(),
                user.getEmail(),
                user.getPassword(),
                true,
                authorities
        );

        return jwtService.generateToken(principal);
    }


    public String bearer(User user, Company company) {
        return "Bearer " + generate(user, company);
    }

    public String bearer(User user, Company company, List<String> permissions) {
        return "Bearer " + generate(user, company, permissions);
    }
}