package org.example.hive.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class MembershipRequestCreate { @NotNull private Long companyId; }
