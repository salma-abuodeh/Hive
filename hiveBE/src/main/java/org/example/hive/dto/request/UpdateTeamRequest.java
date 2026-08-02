package org.example.hive.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateTeamRequest {

    @Size(max = 255)
    private String name;

    @Size(max = 500)
    private String description;

    private Boolean active;
}
