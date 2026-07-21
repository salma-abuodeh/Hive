package org.example.hive.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateJobTitleRequest {

    @Size(max = 150, message = "Title must be at most 150 characters")
    private String title;

    private Boolean active;
}
