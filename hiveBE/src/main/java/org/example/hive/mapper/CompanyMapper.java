package org.example.hive.mapper;

import org.example.hive.dto.request.CompanyRequest;
import org.example.hive.dto.response.CompanyResponse;
import org.example.hive.model.Company;

public class CompanyMapper {

    private CompanyMapper() {
    }

    public static Company toEntity(CompanyRequest request) {
        return Company.builder()
                .name(request.getName())
                .type(request.getType())
                .domain(request.getDomain())
                .logoUrl(request.getLogoUrl())
                .build();
    }

    public static CompanyResponse toResponse(Company company) {
        return CompanyResponse.builder()
                .id(company.getId())
                .name(company.getName())
                .type(company.getType())
                .domain(company.getDomain())
                .logoUrl(company.getLogoUrl())
                .status(company.getStatus())
                .active(company.getActive())
                .createdAt(company.getCreatedAt())
                .updatedAt(company.getUpdatedAt())
                .build();
    }
}