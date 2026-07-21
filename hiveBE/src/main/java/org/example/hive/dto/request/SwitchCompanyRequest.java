package org.example.hive.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SwitchCompanyRequest {

    @NotNull(message = "Company id is required")
    private Long companyId;
}
