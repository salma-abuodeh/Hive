package org.example.hive.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.example.hive.config.AppEnums.RsvpStatus;

@Getter
@Setter
public class RsvpUpdateRequest {

    @NotNull(message = "Status is required")
    private RsvpStatus status;
}