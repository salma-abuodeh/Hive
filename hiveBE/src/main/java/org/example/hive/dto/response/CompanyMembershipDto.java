package org.example.hive.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class CompanyMembershipDto {
    private Long id;
    private String name;
    private String roleName;
    private String jobTitle;
    private Long jobTitleId;
    private List<TeamSummaryDto> teams;
    private Boolean active;
}
