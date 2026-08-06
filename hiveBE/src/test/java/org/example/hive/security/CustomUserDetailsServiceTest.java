package org.example.hive.security;

import org.example.hive.model.Permission;
import org.example.hive.model.Role;
import org.example.hive.model.User;
import org.example.hive.model.UserCompany;
import org.example.hive.model.Company;
import org.example.hive.repository.RolePermissionRepository;
import org.example.hive.repository.UserCompanyRepository;
import org.example.hive.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserCompanyRepository userCompanyRepository;
    @Mock
    private RolePermissionRepository rolePermissionRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    private User user;
    private Role platformRole;
    private Role companyRole;
    private Company company;
    private UserCompany userCompany;

    @BeforeEach
    void setUp() {
        platformRole = new Role();
        platformRole.setId(1L);

        companyRole = new Role();
        companyRole.setId(2L);

        company = new Company();
        company.setId(10L);

        user = new User();
        user.setId(100L);
        user.setEmail("test@hive.local");
        user.setPassword("hashed_pass");
        user.setActive(true);

        userCompany = new UserCompany();
        userCompany.setCompany(company);
        userCompany.setRole(companyRole);
    }

    @Test
    @DisplayName("Should throw UsernameNotFoundException when email does not exist")
    void loadUser_NotFound() {
        when(userRepository.findByEmail("unknown@hive.local")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customUserDetailsService.loadUser("unknown@hive.local", null))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    @DisplayName("Should successfully load UserDetails and map platform + company authorities")
    void loadUser_WithCompanyContext() {
        user.setPlatformRole(platformRole);

        Permission perm1 = new Permission();
        perm1.setName("PLATFORM_MANAGE");

        Permission perm2 = new Permission();
        perm2.setName("COMPANY_VIEW");

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(userCompanyRepository.findByUser_IdAndCompany_IdAndActiveTrue(100L, 10L))
                .thenReturn(Optional.of(userCompany));
        when(rolePermissionRepository.findActivePermissionsByRoleId(1L)).thenReturn(List.of(perm1));
        when(rolePermissionRepository.findActivePermissionsByRoleId(2L)).thenReturn(List.of(perm2));

        UserDetails userDetails = customUserDetailsService.loadUser("test@hive.local", 10L);

        assertThat(userDetails).isInstanceOf(AuthUserPrincipal.class);
        AuthUserPrincipal principal = (AuthUserPrincipal) userDetails;

        assertThat(principal.getUserId()).isEqualTo(100L);
        assertThat(principal.getCompanyId()).isEqualTo(10L);
        assertThat(principal.getUsername()).isEqualTo("test@hive.local");
        assertThat(principal.getAuthorities())
                .extracting("authority")
                .containsExactlyInAnyOrder("PLATFORM_MANAGE", "COMPANY_VIEW");
    }
}