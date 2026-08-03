package org.example.hive.posts;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.hive.config.AppEnums;
import org.example.hive.config.AppEnums.VisibilityType;
import org.example.hive.dto.request.CreateCommentRequest;
import org.example.hive.dto.request.CreatePostRequest;
import org.example.hive.dto.request.ReactToPostRequest;
import org.example.hive.dto.request.UpdatePostRequest;
import org.example.hive.dto.response.PostResponse;
import org.example.hive.integration.BaseIntegrationTest;
import org.example.hive.model.*;
import org.example.hive.repository.PostRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
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
    @Test
    void shouldGetCompanyPostById() throws Exception {

    
    Company company = factory.createCompany();

    Role role = factory.createRole(company);

    User user = factory.createUser();

    factory.assignUserToCompany(user, company, role);

    Post post = factory.createCompanyPost(company, user);

    String token = jwtHelper.generate(user, company);

    mockMvc.perform(get("/posts/{id}", post.getId())
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(post.getId()))
            .andExpect(jsonPath("$.content").value(post.getContent()))
            .andExpect(jsonPath("$.visibilityType").value("COMPANY"))
            .andExpect(jsonPath("$.authorId").value(user.getId()))
            .andExpect(jsonPath("$.authorFirstName").value(user.getFirstName()))
            .andExpect(jsonPath("$.authorLastName").value(user.getLastName()))
            .andExpect(jsonPath("$.teamId").doesNotExist())
            .andExpect(jsonPath("$.likeCount").value(0))
            .andExpect(jsonPath("$.commentCount").value(0))
            .andExpect(jsonPath("$.savedByMe").value(false))
            .andExpect(jsonPath("$.ownedByMe").value(true));
}
    @Test
    void shouldListCompanyFeed() throws Exception {

        Company company = factory.createCompany();

        Role role = factory.createRole(company);

        User user = factory.createUser();

        factory.assignUserToCompany(user, company, role);

        factory.createCompanyPost(company, user);
        factory.createCompanyPost(company, user);

        String token = jwtHelper.generate(user, company);

        mockMvc.perform(get("/posts")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].visibilityType").value("COMPANY"))
                .andExpect(jsonPath("$.content[1].visibilityType").value("COMPANY"));
    }
    @Test
    void shouldListSavedPosts() throws Exception {


        Company company = factory.createCompany();

        Role role = factory.createRole(company);

        User user = factory.createUser();

        factory.assignUserToCompany(user, company, role);

        Post savedPost = factory.createCompanyPost(company, user);

        factory.savePost(user, savedPost);

        String token = jwtHelper.generate(user, company);


        mockMvc.perform(get("/posts/saved")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(savedPost.getId()))
                .andExpect(jsonPath("$.content[0].content").value(savedPost.getContent()))
                .andExpect(jsonPath("$.content[0].savedByMe").value(true))
                .andExpect(jsonPath("$.content[0].ownedByMe").value(true))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }
    @Test
    void shouldLikePost() throws Exception {


        Company company = factory.createCompany();

        Role role = factory.createRole(company);

        User user = factory.createUser();

        factory.assignUserToCompany(user, company, role);

        Post post = factory.createCompanyPost(company, user);

        String token = jwtHelper.generate(user, company);

        ReactToPostRequest request = new ReactToPostRequest();
        request.setReactionType(AppEnums.ReactionType.LIKE);


        mockMvc.perform(post("/posts/{id}/reactions", post.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(post.getId()))
                .andExpect(jsonPath("$.myReaction").value("LIKE"))
                .andExpect(jsonPath("$.likeCount").value(1));
    }

    @Test
    void shouldAddComment() throws Exception {


        Company company = factory.createCompany();

        Role role = factory.createRole(company);

        User user = factory.createUser();

        factory.assignUserToCompany(user, company, role);

        Post post = factory.createCompanyPost(company, user);

        String token = jwtHelper.generate(user, company);

        CreateCommentRequest request = new CreateCommentRequest();
        request.setContent("Nice post!");


        mockMvc.perform(post("/posts/{id}/comments", post.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value("Nice post!"))
                .andExpect(jsonPath("$.authorFirstName").value(user.getFirstName()))
                .andExpect(jsonPath("$.authorLastName").value(user.getLastName()))
                .andExpect(jsonPath("$.ownedByMe").value(true));
    }
    @Test
    void shouldListComments() throws Exception {

        Company company = factory.createCompany();

        Role role = factory.createRole(company);

        User user = factory.createUser();

        factory.assignUserToCompany(user, company, role);

        Post post = factory.createCompanyPost(company, user);

        factory.createComment(post, user);

        String token = jwtHelper.generate(user, company);

        mockMvc.perform(get("/posts/{id}/comments", post.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].content").value("Nice Post!"))
                .andExpect(jsonPath("$[0].authorFirstName").value(user.getFirstName()))
                .andExpect(jsonPath("$[0].authorLastName").value(user.getLastName()));
    }
    //===================================================================
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void shouldSavePost() {


        Company company = factory.createCompany();

        Role role = factory.createRole(company);

        User user = factory.createUser();

        factory.assignUserToCompany(user, company, role);

        Post post = factory.createCompanyPost(company, user);

        String token = jwtHelper.generate(user, company);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<PostResponse> response = restTemplate.exchange(
                "/posts/" + post.getId() + "/save",
                HttpMethod.POST,
                entity,
                PostResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSavedByMe());
        assertEquals(post.getId(), response.getBody().getId());
        assertEquals(post.getContent(), response.getBody().getContent());
    }
}
