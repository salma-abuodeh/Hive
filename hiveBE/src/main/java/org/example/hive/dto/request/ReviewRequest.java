package org.example.hive.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ReviewRequest {
    @Size(max = 500) private String reason;
    private String roleName;
}
