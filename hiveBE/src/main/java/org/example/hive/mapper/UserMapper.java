package org.example.hive.mapper;

import org.example.hive.domain.User;
import org.example.hive.dto.response.UserResponseDto;

public final class UserMapper {

    private UserMapper() {
    }

    public static UserResponseDto toResponse(User user) {
        String roleName = user.getRole() != null ? user.getRole().getName() : null;
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
}
