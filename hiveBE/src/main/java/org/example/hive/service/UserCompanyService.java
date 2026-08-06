package org.example.hive.service;

import org.example.hive.dto.response.CompanySummaryDto;
import org.example.hive.exception.UserException;
import org.example.hive.model.Company;
import org.example.hive.model.Role;
import org.example.hive.model.RoleNames;
import org.example.hive.model.User;
import org.example.hive.model.UserCompany;
import org.example.hive.repository.CompanyRepository;
import org.example.hive.repository.RoleRepository;
import org.example.hive.repository.UserCompanyRepository;
import org.example.hive.security.AuthUserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserCompanyService {

    private final UserCompanyRepository userCompanyRepository;
    private final CompanyRepository companyRepository;
    private final RoleRepository roleRepository;

    public UserCompanyService(UserCompanyRepository userCompanyRepository,
                              CompanyRepository companyRepository,
                              RoleRepository roleRepository) {
        this.userCompanyRepository = userCompanyRepository;
        this.companyRepository = companyRepository;
        this.roleRepository = roleRepository;
    }

    @Transactional
    public UserCompany add(User user, Long companyId, Role role) {
        if (userCompanyRepository.existsByUser_IdAndCompany_Id(user.getId(), companyId)) {
            throw new UserException("User already belongs to this company", HttpStatus.CONFLICT);
        }
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new UserException("Company not found: " + companyId, HttpStatus.NOT_FOUND));

        return userCompanyRepository.save(UserCompany.builder()
                .user(user)
                .company(company)
                .role(role)
                .active(true)
                .build());
    }

    @Transactional
    public UserCompany addAsManager(User user, Company company) {
        Role manager = roleRepository.findByNameAndCompanyIsNull(RoleNames.MANAGER)
                .orElseThrow(() -> new UserException("Manager role not found", HttpStatus.INTERNAL_SERVER_ERROR));
        return add(user, company.getId(), manager);
    }

    @Transactional
    public UserCompany addOrReactivate(User user, Company company, Role role) {
        return userCompanyRepository.findByUser_IdAndCompany_Id(user.getId(), company.getId())
                .map(existing -> {
                    existing.setRole(role);
                    existing.setActive(true);
                    return userCompanyRepository.save(existing);
                })
                .orElseGet(() -> add(user, company.getId(), role));
    }

    @Transactional
    public UserCompany save(UserCompany membership) {
        return userCompanyRepository.save(membership);
    }

    @Transactional
    public void deactivate(UserCompany membership) {
        membership.setActive(false);
        userCompanyRepository.save(membership);
    }

    @Transactional
    public void deleteAllForUser(Long userId) {
        userCompanyRepository.deleteAllByUser_Id(userId);
    }

    @Transactional
    public void updateRoleOnActiveMemberships(Long userId, Role role) {
        for (UserCompany membership : userCompanyRepository.findAllByUser_Id(userId)) {
            if (Boolean.TRUE.equals(membership.getActive())) {
                membership.setRole(role);
                userCompanyRepository.save(membership);
            }
        }
    }

    @Transactional(readOnly = true)
    public List<UserCompany> listActiveForUser(Long userId) {
        return userCompanyRepository.findAllByUser_IdAndActiveTrue(userId);
    }

    @Transactional(readOnly = true)
    public List<CompanySummaryDto> listCompanySummaries(Long userId) {
        return listActiveForUser(userId).stream()
                .map(uc -> new CompanySummaryDto(uc.getCompany().getId(), uc.getCompany().getName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public UserCompany requireInPrincipalCompanies(AuthUserPrincipal principal, Long targetUserId) {
        Set<Long> companyIds = companyIdsOf(principal);
        return userCompanyRepository.findByUser_IdAndCompany_IdInAndActiveTrue(targetUserId, companyIds)
                .orElseThrow(() -> new UserException("User not found", HttpStatus.NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public Page<UserCompany> pageByCompanies(Collection<Long> companyIds, Boolean active, Pageable pageable) {
        if (active == null) {
            return userCompanyRepository.findAllByCompany_IdInAndActiveTrue(companyIds, pageable);
        }
        return userCompanyRepository.findAllByCompany_IdInAndActive(companyIds, active, pageable);
    }

    @Transactional(readOnly = true)
    public Set<Long> companyIdsOf(AuthUserPrincipal principal) {
        if (principal.getCompanyId() != null) {
            return Set.of(principal.getCompanyId());
        }
        return listActiveForUser(principal.getUserId()).stream()
                .map(m -> m.getCompany().getId())
                .collect(Collectors.toSet());
    }

    @Transactional(readOnly = true)
    public UserCompany requireMembership(Long userId, Long companyId) {
        return userCompanyRepository.findByUser_IdAndCompany_Id(userId, companyId)
                .orElseThrow(() -> new UserException("Membership not found", HttpStatus.INTERNAL_SERVER_ERROR));
    }
}
