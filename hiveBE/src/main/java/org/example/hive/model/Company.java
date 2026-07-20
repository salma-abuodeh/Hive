package org.example.hive.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.example.hive.config.AppEnums.CompanyType;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "companies")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true, exclude = {"roles"})
public class Company extends BaseItem {

    @Enumerated(EnumType.STRING)
    @Column(name = "company_type", nullable = false)
    private CompanyType type;

    @Column(unique = true)
    private String domain;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(nullable = false)
    private String status;

    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Role> roles = new ArrayList<>();

    @PrePersist
    protected void onCompanyCreate() {
        if (this.status == null) {
            this.status = "pending";
        }
    }
}