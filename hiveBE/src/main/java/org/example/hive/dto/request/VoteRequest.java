package org.example.hive.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class VoteRequest {

    @NotEmpty(message = "At least one option id is required")
    private List<Long> optionIds;
}