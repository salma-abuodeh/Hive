package org.example.hive.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.security.Principal;
import java.util.Collection;

// Implements Principal too so the same object can sit in a STOMP session
// (accessor.setUser(...)) as well as the regular HttpServletRequest security
// context - one identity object for both entry points.
public class AuthUserPrincipal implements UserDetails, Principal {

    private final Long userId;
    private final Long companyId;
    private final String email;
    private final String password;
    private final boolean active;
    private final Collection<? extends GrantedAuthority> authorities;

    public AuthUserPrincipal(Long userId,
                             Long companyId,
                             String email,
                             String password,
                             boolean active,
                             Collection<? extends GrantedAuthority> authorities) {
        this.userId = userId;
        this.companyId = companyId;
        this.email = email;
        this.password = password;
        this.active = active;
        this.authorities = authorities;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getCompanyId() {
        return companyId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    // java.security.Principal
    @Override
    public String getName() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}