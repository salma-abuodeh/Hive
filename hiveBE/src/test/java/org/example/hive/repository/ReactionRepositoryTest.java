package org.example.hive.repository;

import org.example.hive.config.AppEnums.CompanyType;
import org.example.hive.config.AppEnums.ReactionType;
import org.example.hive.model.Company;
import org.example.hive.model.Post;
import org.example.hive.model.Reaction;
import org.example.hive.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ReactionRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ReactionRepository reactionRepository;

    @Test
    @DisplayName("Should aggregate reaction counts and fetch user-specific reactions for posts")
    void aggregateAndFetchUserReactions() {
        Company company = Company.builder()
                .name("Reaction Test Co.")
                .type(CompanyType.COMPANY)
                .status("active")
                .active(true)
                .build();
        entityManager.persist(company);

        User user = User.builder()
                .firstName("Test")
                .lastName("User")
                .email("reaction-test-user@hive.local")
                .password("password")
                .build();
        entityManager.persist(user);

        Post post = Post.builder()
                .company(company)
                .author(user)
                .content("Post for reaction aggregation test")
                .build();
        entityManager.persist(post);

        Reaction reaction = new Reaction();
        reaction.setPost(post);
        reaction.setUser(user);
        reaction.setReactionType(ReactionType.LIKE);
        entityManager.persist(reaction);

        entityManager.flush();

        // 1. Test Aggregation
        List<Object[]> counts = reactionRepository.countByPostIds(List.of(post.getId()));
        assertThat(counts).hasSize(1);
        assertThat(counts.get(0)[0]).isEqualTo(post.getId());
        assertThat(((Number) counts.get(0)[1]).longValue()).isEqualTo(1L);

        // 2. Test User's Specific Reactions
        List<Object[]> myReactions = reactionRepository.findMyReactionsByPostIds(user.getId(), List.of(post.getId()));
        assertThat(myReactions).hasSize(1);
        assertThat(myReactions.get(0)[0]).isEqualTo(post.getId());
        assertThat(myReactions.get(0)[1]).isEqualTo(ReactionType.LIKE);
    }
}