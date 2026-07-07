package security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import user.Role;
import user.User;
import user.UserRepository;
import user.UserRole;

import java.util.HashSet;
import java.util.Set;

public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("user not found with that email: " + email));
        Set<GrantedAuthority> authorities = new HashSet<>();
        for(UserRole userRole : user.getUserRoles()){
            Role role = userRole.getRole();
            if (role != null){
                authorities.add(new SimpleGrantedAuthority(role.getName()));

            }
        }
        return new AuthUserPrincipal(
                user.getUserId(),
                user.getCompanyId(),
                user.getEmail(),
                user.getPasswordHash(),
                authorities
        );
    }
}
