package org.example.hive.repository;

import org.example.hive.config.AppEnums.PostType;
import org.example.hive.config.AppEnums.VisibilityType;
import org.example.hive.model.Company;
import org.example.hive.model.Post;
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
class PostRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PostRepository postRepository;

    @Test
    @DisplayName("Should fetch feed posts visible to company-wide or user's joined teams")
    void findVisibleFeed() {
        Company company = entityManager.find(Company.class, 1L);
        User author = entityManager.find(User.class, 1L);

        Team team1 = new Team();
        team1.setCompany(company);
        team1.setName("Backend Team");
        entityManager.persist(team1);

        Team team2 = new Team();
        team2.setCompany(company);
        team2.setName("Frontend Team");
        entityManager.persist(team2);

        // Company-wide post
        Post companyPost = new Post();
        companyPost.setCompany(company);
        companyPost.setAuthor(author);
        companyPost.setPostType(PostType.ANNOUNCEMENT);
        companyPost.setContent("Company news");
        companyPost.setVisibilityType(VisibilityType.COMPANY);
        entityManager.persist(companyPost);

        // Team 1 scoped post
        Post team1Post = new Post();
        team1Post.setCompany(company);
        team1Post.setAuthor(author);
        team1Post.setTeam(team1);
        team1Post.setPostType(PostType.TEXT);
        team1Post.setContent("Backend update");
        team1Post.setVisibilityType(VisibilityType.TEAM);
        entityManager.persist(team1Post);

        // Team 2 scoped post
        Post team2Post = new Post();
        team2Post.setCompany(company);
        team2Post.setAuthor(author);
        team2Post.setTeam(team2);
        team2Post.setPostType(PostType.TEXT);
        team2Post.setContent("Frontend update");
        team2Post.setVisibilityType(VisibilityType.TEAM);
        entityManager.persist(team2Post);

        entityManager.flush();

        // User belongs only to Team 1
        Page<Post> feed = postRepository.findVisibleFeed(
                company.getId(),
                List.of(team1.getId()),
                PageRequest.of(0, 10)
        );

        assertThat(feed.getContent()).hasSize(2);
        assertThat(feed.getContent()).extracting(Post::getContent)
                .containsExactlyInAnyOrder("Company news", "Backend update");
    }
}