package org.example.hive.mapper;

import org.example.hive.dto.response.TeamSummaryDto;
import org.example.hive.dto.response.UserResponseDto;
import org.example.hive.model.User;
import org.example.hive.model.UserCompany;

import java.util.List;

public final class UserMapper {

    private UserMapper() {
    }

    public static UserResponseDto toResponse(User user) {
        return toResponse(user, null, null, null, List.of());
    }

    public static UserResponseDto toResponse(UserCompany membership) {
        return toResponse(membership, List.of());
    }

    public static UserResponseDto toResponse(UserCompany membership, List<TeamSummaryDto> teams) {
        Long jobTitleId = membership.getJobTitle() != null ? membership.getJobTitle().getId() : null;
        String jobTitle = membership.getJobTitle() != null
                ? membership.getJobTitle().getTitle()
                : membership.getUser().getJobTitle();
        return new UserResponseDto(
                membership.getUser().getId(),
                membership.getUser().getFirstName(),
                membership.getUser().getLastName(),
                membership.getUser().getEmail(),
                membership.getRole().getName(),
                jobTitle,
                jobTitleId,
                teams != null ? teams : List.of(),
                membership.getActive(),
                membership.getUser().getCreatedAt()
        );
    }

    public static UserResponseDto toResponse(
            User user,
            String roleName,
            String jobTitle,
            Long jobTitleId,
            List<TeamSummaryDto> teams) {
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
                user.getActive(),
                user.getCreatedAt()
        );
    }
}
