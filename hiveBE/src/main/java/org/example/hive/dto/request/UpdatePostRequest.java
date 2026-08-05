package org.example.hive.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.example.hive.config.AppEnums.VisibilityType;

@Getter
@Setter
public class UpdatePostRequest {

    @NotBlank(message = "Content is required")
    @Size(max = 10000, message = "Content must be at most 10000 characters")
    private String content;

    private VisibilityType visibilityType;

    private Long teamId;
}
