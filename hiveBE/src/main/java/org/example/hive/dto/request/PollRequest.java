package org.example.hive.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

    private Long teamId;

    private EventVisibility visibility;

    @NotNull(message = "Options are required")
    @Size(min = 2, max = 10, message = "A poll needs between 2 and 10 options")
    @Valid
    private List<PollOptionRequest> options;
}