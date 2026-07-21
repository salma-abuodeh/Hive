package org.example.hive.security;

import org.example.hive.model.Permission;
import org.example.hive.model.Role;
import org.example.hive.model.User;
import org.example.hive.model.UserCompany;
import org.example.hive.repository.RolePermissionRepository;
import org.example.hive.repository.UserCompanyRepository;
import org.example.hive.repository.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserCompanyRepository userCompanyRepository;
    private final RolePermissionRepository rolePermissionRepository;

    public CustomUserDetailsService(UserRepository userRepository,
                                    UserCompanyRepository userCompanyRepository,
                                    RolePermissionRepository rolePermissionRepository) {
        this.userRepository = userRepository;
        this.userCompanyRepository = userCompanyRepository;
        this.rolePermissionRepository = rolePermissionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        Set<GrantedAuthority> authorities = new LinkedHashSet<>();
        Long companyId = null;

        if (user.getPlatformRole() != null) {
            addPermissions(authorities, user.getPlatformRole());
        }

        Optional<UserCompany> membership =
                userCompanyRepository.findFirstByUser_IdAndActiveTrueOrderByJoinedAtDesc(user.getId());

        if (membership.isPresent()) {
            UserCompany uc = membership.get();
            companyId = uc.getCompany().getId();
            addPermissions(authorities, uc.getRole());
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

    private void addPermissions(Set<GrantedAuthority> authorities, Role role) {
        if (role == null || role.getId() == null) {
            return;
        }
        for (Permission permission : rolePermissionRepository.findActivePermissionsByRoleId(role.getId())) {
            if (permission.getName() != null && !permission.getName().isBlank()) {
                authorities.add(new SimpleGrantedAuthority(permission.getName()));
            }
        }
    }
}
