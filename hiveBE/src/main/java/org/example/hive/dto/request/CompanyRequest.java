package org.example.hive.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.example.hive.config.AppEnums.CompanyType;

@Getter
@Setter
public class CompanyRequest {

    @NotBlank
    private String name;

    @NotNull
    private CompanyType type;

    private String domain;

    private String logoUrl;
}