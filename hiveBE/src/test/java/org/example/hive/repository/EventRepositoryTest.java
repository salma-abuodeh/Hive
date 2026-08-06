package org.example.hive.repository;

import org.example.hive.config.AppEnums.EventVisibility;
import org.example.hive.config.AppEnums.RsvpStatus;
import org.example.hive.model.Company;
import org.example.hive.model.Event;
import org.example.hive.model.EventRsvp;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class EventRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private EventRepository eventRepository;

    @Test
    @DisplayName("Should fetch visible events based on company, team, creator, or RSVP access")
    void findVisibleForUser() {
        Company company = entityManager.find(Company.class, 1L);
        User creator = entityManager.find(User.class, 1L);
        User attendee = entityManager.find(User.class, 2L);

        Team team = new Team();
        team.setCompany(company);
        team.setName("Engineering");
        entityManager.persist(team);

        // 1. Company Event
        Event companyEvent = new Event();
        companyEvent.setCompany(company);
        companyEvent.setCreatedBy(creator);
        companyEvent.setTitle("Company All Hands");
        companyEvent.setStartTime(LocalDateTime.now().plusDays(1));
        companyEvent.setEndTime(LocalDateTime.now().plusDays(1).plusHours(1));
        companyEvent.setVisibility(EventVisibility.COMPANY);
        entityManager.persist(companyEvent);

        // 2. Team Event
        Event teamEvent = new Event();
        teamEvent.setCompany(company);
        teamEvent.setCreatedBy(creator);
        teamEvent.setTeam(team);
        teamEvent.setTitle("Engineering Sync");
        teamEvent.setStartTime(LocalDateTime.now().plusDays(2));
        teamEvent.setEndTime(LocalDateTime.now().plusDays(2).plusHours(1));
        teamEvent.setVisibility(EventVisibility.TEAM);
        entityManager.persist(teamEvent);

        // 3. Private Event (Only creator + RSVP'd users)
        Event privateEvent = new Event();
        privateEvent.setCompany(company);
        privateEvent.setCreatedBy(creator);
        privateEvent.setTitle("Secret Meeting");
        privateEvent.setStartTime(LocalDateTime.now().plusDays(3));
        privateEvent.setEndTime(LocalDateTime.now().plusDays(3).plusHours(1));
        privateEvent.setVisibility(EventVisibility.PRIVATE);
        entityManager.persist(privateEvent);

        // RSVP attendee to private event using RsvpStatus.ACCEPTED
        EventRsvp rsvp = new EventRsvp();
        rsvp.setEvent(privateEvent);
        rsvp.setUser(attendee);
        rsvp.setStatus(RsvpStatus.ACCEPTED);
        entityManager.persist(rsvp);

        entityManager.flush();

        // Test 1: Creator sees all their created events
        Page<Event> creatorEvents = eventRepository.findVisibleForUser(
                company.getId(), creator.getId(), List.of(), false, PageRequest.of(0, 10)
        );
        assertThat(creatorEvents.getContent()).hasSize(3);

        // Test 2: Attendee in team sees Company, Team, and RSVP'd Private event
        Page<Event> attendeeEvents = eventRepository.findVisibleForUser(
                company.getId(), attendee.getId(), List.of(team.getId()), false, PageRequest.of(0, 10)
        );
        assertThat(attendeeEvents.getContent()).extracting(Event::getTitle)
                .containsExactlyInAnyOrder("Company All Hands", "Engineering Sync", "Secret Meeting");

        // Test 3: Override flag sees everything
        Page<Event> adminEvents = eventRepository.findVisibleForUser(
                company.getId(), 999L, List.of(), true, PageRequest.of(0, 10)
        );
        assertThat(adminEvents.getContent()).hasSize(3);
    }
}