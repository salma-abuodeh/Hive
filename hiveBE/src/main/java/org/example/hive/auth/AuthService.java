package org.example.hive.auth;

import org.example.hive.security.AuthUserPrincipal;
import org.example.hive.security.CustomUserDetailsService;
import org.example.hive.security.JwtService;
import org.example.hive.user.User;
import org.example.hive.user.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private  CustomUserDetailsService customUserDetailsService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService, CustomUserDetailsService customUserDetailsService){
        this.jwtService=jwtService;
        this.passwordEncoder=passwordEncoder;
        this.userRepository =userRepository;


        this.customUserDetailsService = customUserDetailsService;
    }

    public LoginResponseDto login(LoginRequestDto req){
        User user =userRepository.findByEmail(req.getEmail()).
                orElseThrow(() -> new RuntimeException(" Invalid Email"));
        if (!(passwordEncoder.matches(req.getPassword(), user.getPasswordHash()))){
            throw new RuntimeException("Invalid Password");
        }

        UserDetails userDetails = customUserDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtService.generateToken(userDetails);
        return new LoginResponseDto(token);
    }
}
