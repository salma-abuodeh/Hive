package org.example.hive.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateCommentRequest {

    @NotBlank(message = "Content is required")
    @Size(max = 2000, message = "Content must be at most 2000 characters")
    private String content;
}
