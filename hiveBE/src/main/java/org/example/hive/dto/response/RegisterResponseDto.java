package org.example.hive.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RegisterResponseDto {
    private String token;
    private Long userId;
    private String email;
    private String firstName;
    private String lastName;
    private Long companyId;
    private String companyName;
}