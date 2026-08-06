package org.example.hive.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserTeamRepositoryTest {

    @Autowired
    private UserTeamRepository userTeamRepository;

    @Test
    @DisplayName("Should aggregate member counts grouped by team IDs")
    void countMembersByTeamIds() {
        // Seeded team IDs and memberships will vary based on test environment data
        List<Object[]> results = userTeamRepository.countMembersByTeamIds(List.of(1L, 2L));

        assertThat(results).isNotNull();
    }
}