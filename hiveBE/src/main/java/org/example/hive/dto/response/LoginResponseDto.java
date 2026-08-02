package org.example.hive.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class LoginResponseDto {
    private String token;
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String role;
    private Boolean active;
    private Long activeCompanyId;
    private List<CompanySummaryDto> companies;
    private List<String> permissions;
}
