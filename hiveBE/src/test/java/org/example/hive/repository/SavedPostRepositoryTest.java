package org.example.hive.repository;

import org.example.hive.config.AppEnums.CompanyType;
import org.example.hive.model.Company;
import org.example.hive.model.Post;
import org.example.hive.model.SavedPost;
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
class SavedPostRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private SavedPostRepository savedPostRepository;

    @Test
    @DisplayName("Should return subset of post IDs that the user has saved")
    void findSavedPostIds() {
        Company company = Company.builder()
                .name("SavedPost Test Co.")
                .type(CompanyType.COMPANY)
                .status("active")
                .active(true)
                .build();
        entityManager.persist(company);

        User user = User.builder()
                .firstName("Test")
                .lastName("User")
                .email("savedpost-test-user@hive.local")
                .password("password")
                .build();
        entityManager.persist(user);

        Post post1 = Post.builder()
                .company(company)
                .author(user)
                .content("Saved post candidate 1")
                .build();
        entityManager.persist(post1);

        Post post2 = Post.builder()
                .company(company)
                .author(user)
                .content("Saved post candidate 2")
                .build();
        entityManager.persist(post2);

        // User saves post 1, but not post 2
        SavedPost savedPost = new SavedPost();
        savedPost.setPost(post1);
        savedPost.setUser(user);
        entityManager.persist(savedPost);
        entityManager.flush();

        List<Long> savedIds = savedPostRepository.findSavedPostIds(user.getId(), List.of(post1.getId(), post2.getId()));

        assertThat(savedIds).hasSize(1);
        assertThat(savedIds).containsExactly(post1.getId());
    }
}