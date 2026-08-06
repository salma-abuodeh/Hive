package org.example.hive.repository;

import org.example.hive.config.AppEnums.EventVisibility;
import org.example.hive.model.Company;
import org.example.hive.model.Poll;
import org.example.hive.model.Team;
import org.example.hive.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PollRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PollRepository pollRepository;

    @Test
    @DisplayName("Should fetch polls visible to the user's company or team context")
    void findVisibleForUser() {
        Company company = entityManager.find(Company.class, 1L);
        User creator = entityManager.find(User.class, 1L);

        Team team = new Team();
        team.setCompany(company);
        team.setName("Marketing");
        entityManager.persist(team);

        Poll companyPoll = new Poll();
        companyPoll.setCompany(company);
        companyPoll.setCreatedBy(creator);
        companyPoll.setQuestion("Company Retreat Location?");
        companyPoll.setVisibility(EventVisibility.COMPANY);
        entityManager.persist(companyPoll);

        Poll teamPoll = new Poll();
        teamPoll.setCompany(company);
        teamPoll.setCreatedBy(creator);
        teamPoll.setTeam(team);
        teamPoll.setQuestion("Marketing Campaign Name?");
        teamPoll.setVisibility(EventVisibility.TEAM);
        entityManager.persist(teamPoll);

        entityManager.flush();

        // User not in team Marketing
        Page<Poll> noTeamFeed = pollRepository.findVisibleForUser(
                company.getId(), 99L, List.of(), false, PageRequest.of(0, 10)
        );
        assertThat(noTeamFeed.getContent()).hasSize(1);
        assertThat(noTeamFeed.getContent().get(0).getQuestion()).isEqualTo("Company Retreat Location?");

        // User in team Marketing
        Page<Poll> teamFeed = pollRepository.findVisibleForUser(
                company.getId(), 99L, List.of(team.getId()), false, PageRequest.of(0, 10)
        );
        assertThat(teamFeed.getContent()).hasSize(2);
    }
}