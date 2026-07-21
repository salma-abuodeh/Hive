package org.example.hive.mapper;

import org.example.hive.dto.response.UserResponseDto;
import org.example.hive.model.User;
import org.example.hive.model.UserCompany;

public final class UserMapper {

    private UserMapper() {
    }

    public static UserResponseDto toResponse(User user) {
        String roleName = user.getPlatformRole() != null ? user.getPlatformRole().getName() : null;
        return new UserResponseDto(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                roleName,
                user.getActive(),
                user.getCreatedAt()
        );
    }

    public static UserResponseDto toResponse(UserCompany membership) {
        return new UserResponseDto(
                membership.getUser().getId(),
                membership.getUser().getFirstName(),
                membership.getUser().getLastName(),
                membership.getUser().getEmail(),
                membership.getRole().getName(),
                membership.getActive(),
                membership.getUser().getCreatedAt()
        );
    }
}
