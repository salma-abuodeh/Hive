package org.example.hive.service;

import org.example.hive.config.AppEnums.PostType;
import org.example.hive.config.AppEnums.ReactionType;
import org.example.hive.config.AppEnums.VisibilityType;
import org.example.hive.dto.request.CreateCommentRequest;
import org.example.hive.dto.request.CreatePostRequest;
import org.example.hive.dto.request.ReactToPostRequest;
import org.example.hive.dto.request.UpdatePostRequest;
import org.example.hive.dto.response.CommentResponse;
import org.example.hive.dto.response.PostResponse;
import org.example.hive.exception.PostException;
import org.example.hive.mapper.PostMapper;
import org.example.hive.model.Comment;
import org.example.hive.model.Company;
import org.example.hive.model.Post;
import org.example.hive.model.Reaction;
import org.example.hive.model.SavedPost;
import org.example.hive.model.Team;
import org.example.hive.model.User;
import org.example.hive.model.UserCompany;
import org.example.hive.repository.CommentRepository;
import org.example.hive.repository.CompanyRepository;
import org.example.hive.repository.PostRepository;
import org.example.hive.repository.ReactionRepository;
import org.example.hive.repository.SavedPostRepository;
import org.example.hive.repository.TeamRepository;
import org.example.hive.repository.UserCompanyRepository;
import org.example.hive.repository.UserRepository;
import org.example.hive.repository.UserTeamRepository;
import org.example.hive.security.AuthUserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PostService {

    private static final Long NO_TEAM_SENTINEL = -1L;

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final ReactionRepository reactionRepository;
    private final SavedPostRepository savedPostRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final TeamRepository teamRepository;
    private final UserTeamRepository userTeamRepository;
    private final UserCompanyRepository userCompanyRepository;

    public PostService(
            PostRepository postRepository,
            CommentRepository commentRepository,
            ReactionRepository reactionRepository,
            SavedPostRepository savedPostRepository,
            UserRepository userRepository,
            CompanyRepository companyRepository,
            TeamRepository teamRepository,
            UserTeamRepository userTeamRepository,
            UserCompanyRepository userCompanyRepository) {
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.reactionRepository = reactionRepository;
        this.savedPostRepository = savedPostRepository;
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.teamRepository = teamRepository;
        this.userTeamRepository = userTeamRepository;
        this.userCompanyRepository = userCompanyRepository;
    }

    @Transactional(readOnly = true)
    public Page<PostResponse> listFeed(AuthUserPrincipal principal, Pageable pageable) {
        Long companyId = requireCompanyId(principal);
        Collection<Long> teamIds = teamIdsForQuery(principal.getUserId(), companyId);
        Page<Post> page = postRepository.findVisibleFeed(companyId, teamIds, pageable);
        return mapPostPage(page, principal, companyId);
    }

    @Transactional(readOnly = true)
    public Page<PostResponse> listSaved(AuthUserPrincipal principal, Pageable pageable) {
        Long companyId = requireCompanyId(principal);
        Collection<Long> teamIds = teamIdsForQuery(principal.getUserId(), companyId);
        Page<Post> page = postRepository.findSavedVisibleFeed(
                principal.getUserId(), companyId, teamIds, pageable);
        return mapPostPage(page, principal, companyId);
    }

    @Transactional(readOnly = true)
    public PostResponse getById(AuthUserPrincipal principal, Long postId) {
        Post post = requireVisiblePost(principal, postId);
        return toResponse(post, principal, requireCompanyId(principal));
    }

    @Transactional
    public PostResponse create(AuthUserPrincipal principal, CreatePostRequest req) {
        Long companyId = requireCompanyId(principal);
        User author = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new PostException("User not found", HttpStatus.NOT_FOUND));
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new PostException("Company not found", HttpStatus.NOT_FOUND));

        Team team = resolveTeam(companyId, principal.getUserId(), req.getVisibilityType(), req.getTeamId());

        Post post = Post.builder()
                .company(company)
                .author(author)
                .team(team)
                .postType(PostType.TEXT)
                .content(req.getContent().trim())
                .visibilityType(req.getVisibilityType())
                .active(true)
                .build();

        return toResponse(postRepository.save(post), principal, companyId);
    }

    @Transactional
    public PostResponse update(AuthUserPrincipal principal, Long postId, UpdatePostRequest req) {
        Post post = requireVisiblePost(principal, postId);
        requireAuthor(principal, post);

        post.setContent(req.getContent().trim());

        VisibilityType visibility = req.getVisibilityType() != null
                ? req.getVisibilityType()
                : post.getVisibilityType();
        Long teamId = req.getTeamId() != null
                ? req.getTeamId()
                : (post.getTeam() != null ? post.getTeam().getId() : null);

        Team team = resolveTeam(requireCompanyId(principal), principal.getUserId(), visibility, teamId);
        post.setVisibilityType(visibility);
        post.setTeam(team);

        return toResponse(postRepository.save(post), principal, requireCompanyId(principal));
    }

    @Transactional
    public void delete(AuthUserPrincipal principal, Long postId) {
        Post post = requireVisiblePost(principal, postId);
        requireAuthor(principal, post);
        post.setActive(false);
        postRepository.save(post);
    }

    @Transactional
    public PostResponse react(AuthUserPrincipal principal, Long postId, ReactToPostRequest req) {
        Post post = requireVisiblePost(principal, postId);
        Long userId = principal.getUserId();
        ReactionType type = req.getReactionType() != null ? req.getReactionType() : ReactionType.LIKE;

        reactionRepository.findByUser_IdAndPost_Id(userId, postId).ifPresentOrElse(existing -> {
            if (existing.getReactionType() == type) {
                reactionRepository.delete(existing);
            } else {
                existing.setReactionType(type);
                reactionRepository.save(existing);
            }
        }, () -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new PostException("User not found", HttpStatus.NOT_FOUND));
            reactionRepository.save(Reaction.builder()
                    .user(user)
                    .post(post)
                    .reactionType(type)
                    .build());
        });

        return toResponse(post, principal, requireCompanyId(principal));
    }

    @Transactional
    public PostResponse removeReaction(AuthUserPrincipal principal, Long postId) {
        Post post = requireVisiblePost(principal, postId);
        reactionRepository.findByUser_IdAndPost_Id(principal.getUserId(), postId)
                .ifPresent(reactionRepository::delete);
        return toResponse(post, principal, requireCompanyId(principal));
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> listComments(AuthUserPrincipal principal, Long postId) {
        requireVisiblePost(principal, postId);
        return commentRepository.findAllByPost_IdAndActiveTrueOrderByCreatedAtAsc(postId).stream()
                .map(c -> PostMapper.toCommentResponse(c, principal.getUserId()))
                .toList();
    }

    @Transactional
    public CommentResponse addComment(AuthUserPrincipal principal, Long postId, CreateCommentRequest req) {
        Post post = requireVisiblePost(principal, postId);
        User user = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new PostException("User not found", HttpStatus.NOT_FOUND));

        Comment comment = commentRepository.save(Comment.builder()
                .post(post)
                .user(user)
                .content(req.getContent().trim())
                .active(true)
                .build());

        return PostMapper.toCommentResponse(comment, principal.getUserId());
    }

    @Transactional
    public void deleteComment(AuthUserPrincipal principal, Long commentId) {
        Comment comment = commentRepository.findByIdAndActiveTrue(commentId)
                .orElseThrow(() -> new PostException("Comment not found", HttpStatus.NOT_FOUND));

        requireVisiblePost(principal, comment.getPost().getId());

        if (!comment.getUser().getId().equals(principal.getUserId())) {
            throw new PostException("Only the comment author can delete it", HttpStatus.FORBIDDEN);
        }

        comment.setActive(false);
        commentRepository.save(comment);
    }

    @Transactional
    public PostResponse savePost(AuthUserPrincipal principal, Long postId) {
        Post post = requireVisiblePost(principal, postId);
        Long userId = principal.getUserId();

        if (!savedPostRepository.existsByUser_IdAndPost_Id(userId, postId)) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new PostException("User not found", HttpStatus.NOT_FOUND));
            savedPostRepository.save(SavedPost.builder()
                    .user(user)
                    .post(post)
                    .build());
        }

        return toResponse(post, principal, requireCompanyId(principal));
    }

    @Transactional
    public PostResponse unsavePost(AuthUserPrincipal principal, Long postId) {
        Post post = requireVisiblePost(principal, postId);
        savedPostRepository.deleteByUser_IdAndPost_Id(principal.getUserId(), postId);
        return toResponse(post, principal, requireCompanyId(principal));
    }

    private Page<PostResponse> mapPostPage(Page<Post> page, AuthUserPrincipal principal, Long companyId) {
        List<Post> posts = page.getContent();
        if (posts.isEmpty()) {
            return page.map(p -> toResponse(p, principal, companyId));
        }

        List<Long> postIds = posts.stream().map(Post::getId).toList();
        Map<Long, Long> likeCounts = toCountMap(
                reactionRepository.countByPostIds(postIds));
        Map<Long, Long> commentCounts = toCountMap(
                commentRepository.countActiveByPostIds(postIds));
        Map<Long, ReactionType> myReactions = toReactionMap(
                reactionRepository.findMyReactionsByPostIds(principal.getUserId(), postIds));
        Set<Long> savedIds = new HashSet<>(
                savedPostRepository.findSavedPostIds(principal.getUserId(), postIds));
        Map<Long, String> roleNames = resolveAuthorRoleNames(posts, companyId);

        return page.map(post -> PostMapper.toResponse(
                post,
                roleNames.get(post.getAuthor().getId()),
                likeCounts.getOrDefault(post.getId(), 0L),
                commentCounts.getOrDefault(post.getId(), 0L),
                myReactions.get(post.getId()),
                savedIds.contains(post.getId()),
                principal.getUserId()));
    }

    private PostResponse toResponse(Post post, AuthUserPrincipal principal, Long companyId) {
        long likes = reactionRepository.countByPost_Id(post.getId());
        long comments = commentRepository.countByPost_IdAndActiveTrue(post.getId());
        ReactionType myReaction = reactionRepository.findByUser_IdAndPost_Id(principal.getUserId(), post.getId())
                .map(Reaction::getReactionType)
                .orElse(null);
        boolean saved = savedPostRepository.existsByUser_IdAndPost_Id(principal.getUserId(), post.getId());
        String roleName = userCompanyRepository
                .findByUser_IdAndCompany_Id(post.getAuthor().getId(), companyId)
                .map(uc -> uc.getRole().getName())
                .orElse(null);

        return PostMapper.toResponse(
                post, roleName, likes, comments, myReaction, saved, principal.getUserId());
    }

    private Map<Long, String> resolveAuthorRoleNames(List<Post> posts, Long companyId) {
        Set<Long> authorIds = posts.stream()
                .map(p -> p.getAuthor().getId())
                .collect(Collectors.toSet());
        Map<Long, String> roles = new HashMap<>();
        for (Long authorId : authorIds) {
            userCompanyRepository.findByUser_IdAndCompany_Id(authorId, companyId)
                    .map(UserCompany::getRole)
                    .ifPresent(role -> roles.put(authorId, role.getName()));
        }
        return roles;
    }

    private static Map<Long, Long> toCountMap(List<Object[]> rows) {
        Map<Long, Long> map = new HashMap<>();
        for (Object[] row : rows) {
            map.put((Long) row[0], (Long) row[1]);
        }
        return map;
    }

    private static Map<Long, ReactionType> toReactionMap(List<Object[]> rows) {
        Map<Long, ReactionType> map = new HashMap<>();
        for (Object[] row : rows) {
            map.put((Long) row[0], (ReactionType) row[1]);
        }
        return map;
    }

    private Post requireVisiblePost(AuthUserPrincipal principal, Long postId) {
        Long companyId = requireCompanyId(principal);
        Collection<Long> teamIds = teamIdsForQuery(principal.getUserId(), companyId);
        return postRepository.findVisibleById(postId, companyId, teamIds)
                .orElseThrow(() -> new PostException("Post not found", HttpStatus.NOT_FOUND));
    }

    private void requireAuthor(AuthUserPrincipal principal, Post post) {
        if (!post.getAuthor().getId().equals(principal.getUserId())) {
            throw new PostException("Only the author can modify this post", HttpStatus.FORBIDDEN);
        }
    }

    private Long requireCompanyId(AuthUserPrincipal principal) {
        if (principal.getCompanyId() == null) {
            throw new PostException("Join or create a company to use the feed", HttpStatus.BAD_REQUEST);
        }
        return principal.getCompanyId();
    }

    private Collection<Long> teamIdsForQuery(Long userId, Long companyId) {
        List<Long> ids = userTeamRepository.findTeamIdsByUserAndCompany(userId, companyId);
        if (ids.isEmpty()) {
            return Collections.singletonList(NO_TEAM_SENTINEL);
        }
        return ids;
    }

    private Team resolveTeam(Long companyId, Long userId, VisibilityType visibility, Long teamId) {
        if (visibility == VisibilityType.COMPANY) {
            return null;
        }
        if (visibility != VisibilityType.TEAM) {
            throw new PostException("Unsupported visibility", HttpStatus.BAD_REQUEST);
        }
        if (teamId == null) {
            throw new PostException("Team is required for team visibility", HttpStatus.BAD_REQUEST);
        }
        Team team = teamRepository.findByIdAndCompany_IdAndActiveTrue(teamId, companyId)
                .orElseThrow(() -> new PostException("Team not found", HttpStatus.NOT_FOUND));
        boolean member = userTeamRepository.existsByUser_IdAndTeam_Id(userId, teamId);
        if (!member) {
            throw new PostException("You must be a member of the team", HttpStatus.FORBIDDEN);
        }
        return team;
    }
}
