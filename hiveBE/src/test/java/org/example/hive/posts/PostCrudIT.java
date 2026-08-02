package org.example.hive.posts;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.hive.config.AppEnums.VisibilityType;
import org.example.hive.dto.request.CreatePostRequest;
import org.example.hive.dto.request.UpdatePostRequest;
import org.example.hive.integration.BaseIntegrationTest;
import org.example.hive.model.*;
import org.example.hive.repository.PostRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class PostCrudIT extends BaseIntegrationTest {
    @Autowired
    private PostRepository postRepository;
    @Test
    void shouldCreateCompanyPost() throws Exception {



        Company company = factory.createCompany();

        Role role = factory.createRole(company);

        User user = factory.createUser();

        factory.assignUserToCompany(user, company, role);

        String token = jwtHelper.generate(user, company);

        CreatePostRequest request = new CreatePostRequest();
        request.setContent("Hello from integration test");
        request.setVisibilityType(VisibilityType.COMPANY);



        mockMvc.perform(
                        post("/posts")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content")
                        .value("Hello from integration test"))
                .andExpect(jsonPath("$.visibilityType")
                        .value("COMPANY"))
                .andExpect(jsonPath("$.authorFirstName")
                        .value("John"))
                .andExpect(jsonPath("$.authorLastName")
                        .value("Doe"))
                .andExpect(jsonPath("$.ownedByMe")
                        .value(true));

    }
    @Test
    void shouldCreateTeamPost() throws Exception {


        Company company = factory.createCompany();

        Role role = factory.createRole(company);

        User user = factory.createUser();

        factory.assignUserToCompany(user, company, role);

        Team team = factory.createTeam(company);

        factory.addUserToTeam(user, team);

        String token = jwtHelper.generate(user, company);

        CreatePostRequest request = new CreatePostRequest();
        request.setContent("Hello Backend Team!");
        request.setVisibilityType(VisibilityType.TEAM);
        request.setTeamId(team.getId());

        mockMvc.perform(post("/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value("Hello Backend Team!"))
                .andExpect(jsonPath("$.visibilityType").value("TEAM"))
                .andExpect(jsonPath("$.teamId").value(team.getId()))
                .andExpect(jsonPath("$.teamName").value(team.getName()))
                .andExpect(jsonPath("$.authorFirstName").value(user.getFirstName()))
                .andExpect(jsonPath("$.authorLastName").value(user.getLastName()))
                .andExpect(jsonPath("$.ownedByMe").value(true))
                .andExpect(jsonPath("$.savedByMe").value(false))
                .andExpect(jsonPath("$.likeCount").value(0))
                .andExpect(jsonPath("$.commentCount").value(0));
    }
    @Test
    void shouldUpdateOwnPost() throws Exception {

        Company company = factory.createCompany();

        Role role = factory.createRole(company);

        User user = factory.createUser();

        factory.assignUserToCompany(user, company, role);

        Post post = factory.createCompanyPost(company, user);

        String token = jwtHelper.generate(user, company);

        UpdatePostRequest request = new UpdatePostRequest();
        request.setContent("Updated post content");
        request.setVisibilityType(VisibilityType.COMPANY);

        mockMvc.perform(put("/posts/{id}", post.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(post.getId()))
                .andExpect(jsonPath("$.content").value("Updated post content"))
                .andExpect(jsonPath("$.visibilityType").value("COMPANY"))
                .andExpect(jsonPath("$.authorFirstName").value(user.getFirstName()))
                .andExpect(jsonPath("$.authorLastName").value(user.getLastName()))
                .andExpect(jsonPath("$.ownedByMe").value(true));
    }
    @Test
    void shouldDeleteOwnPost() throws Exception {

        // Arrange
        Company company = factory.createCompany();

        Role role = factory.createRole(company);

        User user = factory.createUser();

        factory.assignUserToCompany(user, company, role);

        Post post = factory.createCompanyPost(company, user);

        String token = jwtHelper.generate(user, company);

        mockMvc.perform(delete("/posts/{id}", post.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        Post deleted = postRepository.findById(post.getId())
                .orElseThrow();

        assertThat(deleted.getActive()).isFalse();
    }
}