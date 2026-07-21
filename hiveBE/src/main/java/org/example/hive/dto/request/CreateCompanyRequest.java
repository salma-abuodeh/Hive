package org.example.hive.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.example.hive.config.AppEnums.CompanyType;

@Getter
@Setter
public class CreateCompanyRequest {
    @NotBlank(message = "Company name is required")
    @Size(max = 255)
    private String name;

    @NotNull(message = "Company type is required")
    private CompanyType type;

    @Size(max = 255)
    private String domain;
}
