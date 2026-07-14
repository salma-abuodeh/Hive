package org.example.hive.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RegisterResponseDto {
    private String token;
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String role;
    private Boolean active;
}