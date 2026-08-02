package org.example.hive.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.example.hive.config.AppEnums.EventVisibility;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class PollRequest {

    @NotBlank(message = "Question is required")
    @Size(max = 255)
    private String question;

    private String description;

    private Boolean allowMultiple;

    private LocalDateTime closesAt;

    private EventVisibility visibility;

    @Size(min = 2, message = "A poll needs at least 2 options")
    @Valid
    private List<PollOptionRequest> options;
}