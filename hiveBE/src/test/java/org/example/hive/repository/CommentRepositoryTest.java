package org.example.hive.repository;

import org.example.hive.config.AppEnums.CompanyType;
import org.example.hive.model.Comment;
import org.example.hive.model.Company;
import org.example.hive.model.Post;
import org.example.hive.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CommentRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CommentRepository commentRepository;

    @Test
    @DisplayName("Should aggregate active comment counts mapped by post ID")
    void countActiveByPostIds() {
        Company company = Company.builder()
                .name("Comment Test Co.")
                .type(CompanyType.COMPANY)
                .status("active")
                .active(true)
                .build();
        entityManager.persist(company);

        User user = User.builder()
                .firstName("Test")
                .lastName("User")
                .email("comment-test-user@hive.local")
                .password("password")
                .build();
        entityManager.persist(user);

        Post post1 = Post.builder()
                .company(company)
                .author(user)
                .content("Post for comment aggregation test")
                .build();
        entityManager.persist(post1);

        Comment c1 = new Comment();
        c1.setPost(post1);
        c1.setUser(user);
        c1.setContent("First!");
        entityManager.persist(c1);

        Comment c2 = new Comment();
        c2.setPost(post1);
        c2.setUser(user);
        c2.setContent("Second!");
        entityManager.persist(c2);

        entityManager.flush();

        List<Object[]> counts = commentRepository.countActiveByPostIds(List.of(post1.getId()));

        assertThat(counts).hasSize(1);
        assertThat(counts.get(0)[0]).isEqualTo(post1.getId()); // Post ID
        assertThat(((Number) counts.get(0)[1]).longValue()).isEqualTo(2L); // Count
    }
}
