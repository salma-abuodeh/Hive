package org.example.hive.util;

import org.example.hive.config.AppEnums.CompanyType;
import org.example.hive.config.AppEnums.PostType;
import org.example.hive.config.AppEnums.VisibilityType;
import org.example.hive.model.*;
import org.example.hive.repository.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class TestDataFactory {

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final TeamRepository teamRepository;
    private final UserCompanyRepository userCompanyRepository;
    private final UserTeamRepository userTeamRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final SavedPostRepository savedPostRepository;

    public TestDataFactory(
            CompanyRepository companyRepository,
            UserRepository userRepository,
            RoleRepository roleRepository,
            TeamRepository teamRepository,
            UserCompanyRepository userCompanyRepository,
            UserTeamRepository userTeamRepository,
            PostRepository postRepository,
            CommentRepository commentRepository,
            SavedPostRepository savedPostRepository
    ) {
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.teamRepository = teamRepository;
        this.userCompanyRepository = userCompanyRepository;
        this.userTeamRepository = userTeamRepository;
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.savedPostRepository = savedPostRepository;
    }

    public Company createCompany() {

        Company company = Company.builder()
                .name("Test Company")
                .type(CompanyType.COMPANY)
                .domain("test-company.com")
                .logoUrl(null)
                .build();

        return companyRepository.save(company);
    }

    public Role createRole(Company company) {

        Role role = Role.builder()
                .name("Manager")
                .description("Manager Role")
                .company(company)
                .build();

        return roleRepository.save(role);
    }

    public User createUser() {

        User user = User.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john" + System.nanoTime() + "@mail.com")
                .password("$2a$10$abcdefghijklmnopqrstuv")
                .jobTitle("Software Engineer")
                .profileImageUrl(null)
                .build();

        return userRepository.save(user);
    }

    public UserCompany assignUserToCompany(
            User user,
            Company company,
            Role role
    ) {

        UserCompany uc = UserCompany.builder()
                .user(user)
                .company(company)
                .role(role)
                .jobTitle("Software Engineer")
                .build();

        return userCompanyRepository.save(uc);
    }

    public Team createTeam(Company company) {

        Team team = Team.builder()
                .name("Backend")
                .description("Backend Team")
                .company(company)
                .build();

        return teamRepository.save(team);
    }

    public UserTeam addUserToTeam(User user, Team team) {

        UserTeam ut = UserTeam.builder()
                .user(user)
                .team(team)
                .build();

        return userTeamRepository.save(ut);
    }

    public Post createCompanyPost(
            Company company,
            User author
    ) {

        Post post = Post.builder()
                .company(company)
                .author(author)
                .content("Hello World")
                .visibilityType(VisibilityType.COMPANY)
                .postType(PostType.TEXT)
                .build();

        return postRepository.save(post);
    }

    public Post createTeamPost(
            Company company,
            User author,
            Team team
    ) {

        Post post = Post.builder()
                .company(company)
                .author(author)
                .team(team)
                .content("Hello Team")
                .visibilityType(VisibilityType.TEAM)
                .postType(PostType.TEXT)
                .build();

        return postRepository.save(post);
    }

    public Comment createComment(
            Post post,
            User user
    ) {

        Comment comment = Comment.builder()
                .post(post)
                .user(user)
                .content("Nice Post!")
                .build();

        return commentRepository.save(comment);
    }

    public SavedPost savePost(
            User user,
            Post post
    ) {

        SavedPost saved = SavedPost.builder()
                .user(user)
                .post(post)
                .build();

        return savedPostRepository.save(saved);
    }

}