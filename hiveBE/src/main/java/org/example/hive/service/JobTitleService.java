package org.example.hive.service;

import org.example.hive.dto.request.CreateJobTitleRequest;
import org.example.hive.dto.request.UpdateJobTitleRequest;
import org.example.hive.dto.response.JobTitleResponse;
import org.example.hive.exception.JobTitleException;
import org.example.hive.model.Company;
import org.example.hive.model.CompanyJobTitle;
import org.example.hive.repository.CompanyJobTitleRepository;
import org.example.hive.repository.CompanyRepository;
import org.example.hive.security.AuthUserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class JobTitleService {

    private final CompanyJobTitleRepository jobTitleRepository;
    private final CompanyRepository companyRepository;

    public JobTitleService(CompanyJobTitleRepository jobTitleRepository,
                           CompanyRepository companyRepository) {
        this.jobTitleRepository = jobTitleRepository;
        this.companyRepository = companyRepository;
    }

    @Transactional(readOnly = true)
    public List<JobTitleResponse> listMine(AuthUserPrincipal principal, boolean includeInactive) {
        Long companyId = requireCompanyId(principal);
        List<CompanyJobTitle> titles = includeInactive
                ? jobTitleRepository.findAllByCompany_IdOrderByTitleAsc(companyId)
                : jobTitleRepository.findAllByCompany_IdAndActiveTrueOrderByTitleAsc(companyId);
        return titles.stream().map(this::toResponse).toList();
    }

    @Transactional
    public JobTitleResponse create(AuthUserPrincipal principal, CreateJobTitleRequest req) {
        Long companyId = requireCompanyId(principal);
        String title = req.getTitle().trim();

        if (jobTitleRepository.existsByCompany_IdAndTitleIgnoreCase(companyId, title)) {
            throw new JobTitleException("Job title already exists", HttpStatus.CONFLICT);
        }

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new JobTitleException("Company not found", HttpStatus.NOT_FOUND));

        CompanyJobTitle entity = jobTitleRepository.save(CompanyJobTitle.builder()
                .company(company)
                .title(title)
                .active(true)
                .build());

        return toResponse(entity);
    }

    @Transactional
    public JobTitleResponse update(AuthUserPrincipal principal, Long id, UpdateJobTitleRequest req) {
        Long companyId = requireCompanyId(principal);
        CompanyJobTitle entity = jobTitleRepository.findByIdAndCompany_Id(id, companyId)
                .orElseThrow(() -> new JobTitleException("Job title not found", HttpStatus.NOT_FOUND));

        if (req.getTitle() != null && !req.getTitle().isBlank()) {
            String title = req.getTitle().trim();
            if (!title.equalsIgnoreCase(entity.getTitle())
                    && jobTitleRepository.existsByCompany_IdAndTitleIgnoreCase(companyId, title)) {
                throw new JobTitleException("Job title already exists", HttpStatus.CONFLICT);
            }
            entity.setTitle(title);
        }
        if (req.getActive() != null) {
            entity.setActive(req.getActive());
        }

        return toResponse(jobTitleRepository.save(entity));
    }

    @Transactional
    public void deactivate(AuthUserPrincipal principal, Long id) {
        Long companyId = requireCompanyId(principal);
        CompanyJobTitle entity = jobTitleRepository.findByIdAndCompany_Id(id, companyId)
                .orElseThrow(() -> new JobTitleException("Job title not found", HttpStatus.NOT_FOUND));
        entity.setActive(false);
        jobTitleRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public CompanyJobTitle requireActiveForCompany(Long jobTitleId, Long companyId) {
        return jobTitleRepository.findByIdAndCompany_IdAndActiveTrue(jobTitleId, companyId)
                .orElseThrow(() -> new JobTitleException("Job title not found", HttpStatus.NOT_FOUND));
    }

    private Long requireCompanyId(AuthUserPrincipal principal) {
        if (principal.getCompanyId() == null) {
            throw new JobTitleException("Join or create a company to manage job titles", HttpStatus.BAD_REQUEST);
        }
        return principal.getCompanyId();
    }

    private JobTitleResponse toResponse(CompanyJobTitle entity) {
        return JobTitleResponse.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .active(entity.getActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
