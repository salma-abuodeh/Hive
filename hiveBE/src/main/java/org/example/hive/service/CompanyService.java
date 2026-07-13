package org.example.hive.service;

import org.example.hive.dto.request.CompanyRequest;
import org.example.hive.dto.response.CompanyResponse;
import org.example.hive.exception.CompanyException;
import org.example.hive.mapper.CompanyMapper;
import org.example.hive.model.Company;
import org.example.hive.repository.CompanyRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompanyService {

    private final CompanyRepository companyRepository;

    public CompanyService(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    // Not tenant-scoped: no company exists yet to scope against.
    // TODO: restrict this to a platform-admin role once that role exists.
    @Transactional
    public CompanyResponse create(CompanyRequest req) {
        Company company = CompanyMapper.toEntity(req);
        return CompanyMapper.toResponse(companyRepository.save(company));
    }

    @Transactional(readOnly = true)
    public CompanyResponse getById(Long companyId) {
        return CompanyMapper.toResponse(findCompany(companyId));
    }

    @Transactional
    public CompanyResponse update(Long companyId, CompanyRequest req) {
        Company company = findCompany(companyId);

        company.setName(req.getName());
        company.setType(req.getType());
        company.setDomain(req.getDomain());
        company.setLogoUrl(req.getLogoUrl());

        return CompanyMapper.toResponse(companyRepository.save(company));
    }

    @Transactional
    public void archive(Long companyId) {
        Company company = findCompany(companyId);
        company.setActive(false);
        company.setStatus("archived");
        companyRepository.save(company);
    }

    private Company findCompany(Long id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new CompanyException("Company not found", HttpStatus.NOT_FOUND));
    }
}