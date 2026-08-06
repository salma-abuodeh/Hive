package org.example.hive.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PollOptionRequest {

    @NotBlank(message = "Option text is required")
    @Size(max = 255)
    private String text;
}