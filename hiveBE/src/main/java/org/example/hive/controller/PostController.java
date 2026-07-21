package org.example.hive.controller;

import jakarta.validation.Valid;
import org.example.hive.dto.request.CreateCommentRequest;
import org.example.hive.dto.request.CreatePostRequest;
import org.example.hive.dto.request.UpdatePostRequest;
import org.example.hive.dto.response.CommentResponse;
import org.example.hive.dto.response.PostResponse;
import org.example.hive.security.AuthUserPrincipal;
import org.example.hive.service.PostService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/posts")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public Page<PostResponse> listFeed(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            Pageable pageable) {
        return postService.listFeed(principal, pageable);
    }

    @GetMapping("/saved")
    @PreAuthorize("isAuthenticated()")
    public Page<PostResponse> listSaved(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            Pageable pageable) {
        return postService.listSaved(principal, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public PostResponse getById(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthUserPrincipal principal) {
        return postService.getById(principal, id);
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.CREATED)
    public PostResponse create(
            @Valid @RequestBody CreatePostRequest req,
            @AuthenticationPrincipal AuthUserPrincipal principal) {
        return postService.create(principal, req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public PostResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePostRequest req,
            @AuthenticationPrincipal AuthUserPrincipal principal) {
        return postService.update(principal, id, req);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthUserPrincipal principal) {
        postService.delete(principal, id);
    }

    @PostMapping("/{id}/reactions")
    @PreAuthorize("isAuthenticated()")
    public PostResponse like(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthUserPrincipal principal) {
        return postService.like(principal, id);
    }

    @DeleteMapping("/{id}/reactions")
    @PreAuthorize("isAuthenticated()")
    public PostResponse unlike(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthUserPrincipal principal) {
        return postService.unlike(principal, id);
    }

    @GetMapping("/{id}/comments")
    @PreAuthorize("isAuthenticated()")
    public List<CommentResponse> listComments(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthUserPrincipal principal) {
        return postService.listComments(principal, id);
    }

    @PostMapping("/{id}/comments")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse addComment(
            @PathVariable Long id,
            @Valid @RequestBody CreateCommentRequest req,
            @AuthenticationPrincipal AuthUserPrincipal principal) {
        return postService.addComment(principal, id, req);
    }

    @DeleteMapping("/comments/{commentId}")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal AuthUserPrincipal principal) {
        postService.deleteComment(principal, commentId);
    }

    @PostMapping("/{id}/save")
    @PreAuthorize("isAuthenticated()")
    public PostResponse savePost(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthUserPrincipal principal) {
        return postService.savePost(principal, id);
    }

    @DeleteMapping("/{id}/save")
    @PreAuthorize("isAuthenticated()")
    public PostResponse unsavePost(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthUserPrincipal principal) {
        return postService.unsavePost(principal, id);
    }
}
