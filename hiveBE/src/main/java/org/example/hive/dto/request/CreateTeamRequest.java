package org.example.hive.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateTeamRequest {

    @NotBlank(message = "Team name is required")
    @Size(max = 255)
    private String name;

    @Size(max = 500)
    private String description;
}
