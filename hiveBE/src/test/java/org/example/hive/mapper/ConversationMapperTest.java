package org.example.hive.mapper;

import org.example.hive.config.AppEnums.ConversationType;
import org.example.hive.dto.response.ConversationResponse;
import org.example.hive.model.Company;
import org.example.hive.model.Conversation;
import org.example.hive.model.Team;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConversationMapperTest {

    @Test
    void usesTheTeamNameForTeamConversations() {
        Company company = Company.builder().id(1L).name("Hive").build();
        Team team = Team.builder().id(2L).name("Engineering").company(company).build();
        Conversation conversation = Conversation.builder()
                .id(3L)
                .company(company)
                .conversationType(ConversationType.TEAM)
                .team(team)
                .build();

        ConversationResponse response = ConversationMapper.toResponse(conversation, List.of(), null, 0);

        assertThat(response.getName()).isEqualTo("Engineering");
    }
}
