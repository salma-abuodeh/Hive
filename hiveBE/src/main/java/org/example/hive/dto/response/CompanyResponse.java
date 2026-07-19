package org.example.hive.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.example.hive.config.AppEnums.CompanyType;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class CompanyResponse {

    private Long id;
    private String name;
    private CompanyType type;
    private String domain;
    private String logoUrl;
    private String status;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}