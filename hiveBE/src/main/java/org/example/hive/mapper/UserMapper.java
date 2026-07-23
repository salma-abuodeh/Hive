package org.example.hive.mapper;

import org.example.hive.dto.response.CompanyMembershipDto;
import org.example.hive.dto.response.TeamSummaryDto;
import org.example.hive.dto.response.UserResponseDto;
import org.example.hive.model.User;
import org.example.hive.model.UserCompany;

import java.util.List;

public final class UserMapper {

    private UserMapper() {
    }

    public static UserResponseDto toResponse(User user) {
        return toResponse(user, null, null, null, List.of(), null, List.of());
    }

    public static UserResponseDto toResponse(UserCompany membership) {
        return toResponse(membership, List.of());
    }

    public static UserResponseDto toResponse(UserCompany membership, List<TeamSummaryDto> teams) {
        Long jobTitleId = membership.getJobTitle() != null ? membership.getJobTitle().getId() : null;
        String jobTitle = membership.getJobTitle() != null
                ? membership.getJobTitle().getTitle()
                : membership.getUser().getJobTitle();
        Long companyId = membership.getCompany().getId();
        return new UserResponseDto(
                membership.getUser().getId(),
                membership.getUser().getFirstName(),
                membership.getUser().getLastName(),
                membership.getUser().getEmail(),
                membership.getRole().getName(),
                jobTitle,
                jobTitleId,
                teams != null ? teams : List.of(),
                companyId,
                List.of(),
                membership.getActive(),
                membership.getUser().getCreatedAt()
        );
    }

    public static UserResponseDto toProfile(
            User user,
            UserCompany activeMembership,
            List<TeamSummaryDto> activeTeams,
            Long activeCompanyId,
            List<CompanyMembershipDto> companies) {
        if (activeMembership != null) {
            Long jobTitleId = activeMembership.getJobTitle() != null
                    ? activeMembership.getJobTitle().getId()
                    : null;
            String jobTitle = activeMembership.getJobTitle() != null
                    ? activeMembership.getJobTitle().getTitle()
                    : user.getJobTitle();
            return new UserResponseDto(
                    user.getId(),
                    user.getFirstName(),
                    user.getLastName(),
                    user.getEmail(),
                    activeMembership.getRole().getName(),
                    jobTitle,
                    jobTitleId,
                    activeTeams != null ? activeTeams : List.of(),
                    activeCompanyId,
                    companies != null ? companies : List.of(),
                    activeMembership.getActive(),
                    user.getCreatedAt()
            );
        }

        String platformRole = user.getPlatformRole() != null ? user.getPlatformRole().getName() : null;
        return new UserResponseDto(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                platformRole,
                user.getJobTitle(),
                null,
                List.of(),
                activeCompanyId,
                companies != null ? companies : List.of(),
                user.getActive(),
                user.getCreatedAt()
        );
    }

    public static UserResponseDto toResponse(
            User user,
            String roleName,
            String jobTitle,
            Long jobTitleId,
            List<TeamSummaryDto> teams) {
        return toResponse(user, roleName, jobTitle, jobTitleId, teams, null, List.of());
    }

    public static UserResponseDto toResponse(
            User user,
            String roleName,
            String jobTitle,
            Long jobTitleId,
            List<TeamSummaryDto> teams,
            Long activeCompanyId,
            List<CompanyMembershipDto> companies) {
        String resolvedRole = roleName != null
                ? roleName
                : (user.getPlatformRole() != null ? user.getPlatformRole().getName() : null);
        return new UserResponseDto(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                resolvedRole,
                jobTitle != null ? jobTitle : user.getJobTitle(),
                jobTitleId,
                teams != null ? teams : List.of(),
                activeCompanyId,
                companies != null ? companies : List.of(),
                user.getActive(),
                user.getCreatedAt()
        );
    }

    public static CompanyMembershipDto toCompanyMembership(
            UserCompany membership,
            List<TeamSummaryDto> teams) {
        Long jobTitleId = membership.getJobTitle() != null ? membership.getJobTitle().getId() : null;
        String jobTitle = membership.getJobTitle() != null
                ? membership.getJobTitle().getTitle()
                : null;
        return new CompanyMembershipDto(
                membership.getCompany().getId(),
                membership.getCompany().getName(),
                membership.getRole() != null ? membership.getRole().getName() : null,
                jobTitle,
                jobTitleId,
                teams != null ? teams : List.of(),
                membership.getActive()
        );
    }
}
