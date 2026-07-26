package org.example.hive.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UpdateUserRequest {

    @Size(max = 255)
    private String firstName;

    @Size(max = 255)
    private String lastName;

    @Email(message = "Email must be valid")
    private String email;

    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    private String roleName;

    private Boolean active;

    @Size(max = 150)
    private String jobTitle;

    private List<Long> teamIds;
}
