package org.example.hive.security;

import org.example.hive.model.User;
import org.example.hive.model.UserCompany;
import org.example.hive.repository.UserCompanyRepository;
import org.example.hive.repository.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserCompanyRepository userCompanyRepository;

    public CustomUserDetailsService(UserRepository userRepository,
                                    UserCompanyRepository userCompanyRepository) {
        this.userRepository = userRepository;
        this.userCompanyRepository = userCompanyRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        List<GrantedAuthority> authorities = new ArrayList<>();
        Long companyId = null;

        if (user.getPlatformRole() != null) {
            authorities.add(toAuthority(user.getPlatformRole().getName()));
        }

        Optional<UserCompany> membership =
                userCompanyRepository.findFirstByUser_IdAndActiveTrueOrderByJoinedAtDesc(user.getId());

        if (membership.isPresent()) {
            UserCompany uc = membership.get();
            companyId = uc.getCompany().getId();
            authorities.add(toAuthority(uc.getRole().getName()));
        }

        if (authorities.isEmpty()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        }

        return new AuthUserPrincipal(
                user.getId(),
                companyId,
                user.getEmail(),
                user.getPassword(),
                Boolean.TRUE.equals(user.getActive()),
                authorities
        );
    }

    private GrantedAuthority toAuthority(String roleName) {
        return new SimpleGrantedAuthority("ROLE_" + roleName.replace(" ", "_").toUpperCase());
    }
}