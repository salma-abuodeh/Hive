package org.example.hive.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class UserResponseDto {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String roleName;
    private String jobTitle;
    private List<TeamSummaryDto> teams;
    private Long activeCompanyId;
    private List<CompanyMembershipDto> companies;
    private Boolean active;
    private LocalDateTime createdAt;
    private String avatarUrl;
}