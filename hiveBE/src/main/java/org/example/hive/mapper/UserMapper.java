package org.example.hive.mapper;

import org.example.hive.model.UserCompany;
import org.example.hive.dto.response.UserResponseDto;

public final class UserMapper {

    private UserMapper() {
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