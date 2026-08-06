package org.example.hive.service;

import org.example.hive.dto.request.CompanyRequest;
import org.example.hive.dto.request.CreateCompanyRequest;
import org.example.hive.dto.response.CompanyResponse;
import org.example.hive.exception.CompanyException;
import org.example.hive.exception.UserException;
import org.example.hive.mapper.CompanyMapper;
import org.example.hive.model.Company;
import org.example.hive.model.RoleNames;
import org.example.hive.model.User;
import org.example.hive.repository.CompanyRepository;
import org.example.hive.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final UserCompanyService userCompanyService;

    public CompanyService(CompanyRepository companyRepository,
                          UserRepository userRepository,
                          UserCompanyService userCompanyService) {
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
        this.userCompanyService = userCompanyService;
    }

    @Transactional
    public CompanyResponse createForUser(Long userId, CreateCompanyRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException("User not found", HttpStatus.NOT_FOUND));

        String domain = req.getDomain() == null ? null : req.getDomain().trim().toLowerCase();
        if (domain != null && !domain.isBlank() && companyRepository.existsByDomain(domain)) {
            throw new UserException("A company with this domain already exists", HttpStatus.CONFLICT);
        }

        Company company = companyRepository.save(Company.builder()
                .name(req.getName().trim())
                .type(req.getType())
                .domain(domain == null || domain.isBlank() ? null : domain)
                .build());

        if (!isPlatformAdmin(user)) {
            userCompanyService.addAsManager(user, company);
        }

        return CompanyMapper.toResponse(company);
    }

    @Transactional
    public CompanyResponse create(CompanyRequest req) {
        String domain = req.getDomain() == null ? null : req.getDomain().trim().toLowerCase();
        if (domain != null && !domain.isBlank() && companyRepository.existsByDomain(domain)) {
            throw new CompanyException("A company with this domain already exists", HttpStatus.CONFLICT);
        }

        Company company = CompanyMapper.toEntity(req);
        if (domain != null && !domain.isBlank()) {
            company.setDomain(domain);
        }
        return CompanyMapper.toResponse(companyRepository.save(company));
    }

    @Transactional(readOnly = true)
    public CompanyResponse getById(Long companyId) {
        return CompanyMapper.toResponse(findCompany(companyId));
    }

    @Transactional(readOnly = true)
    public List<CompanyResponse> listJoinable(Long userId) {
        return companyRepository.findByActiveTrue().stream()
                .filter(c -> "active".equalsIgnoreCase(c.getStatus()))
                .filter(c -> userCompanyService.listActiveForUser(userId).stream()
                        .noneMatch(m -> m.getCompany().getId().equals(c.getId())))
                .map(CompanyMapper::toResponse)
                .toList();
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

    private boolean isPlatformAdmin(User user) {
        if (user.getPlatformRole() == null || user.getPlatformRole().getName() == null) {
            return false;
        }
        return RoleNames.PLATFORM_ADMIN.equals(user.getPlatformRole().getName());
    }
}
