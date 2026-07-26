import { DatePipe, isPlatformBrowser } from '@angular/common';
import { Component, OnInit, PLATFORM_ID, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import { TeamService } from '../../../../core/services/team.service';
import { Team } from '../../../company/models/team.models';
import { PostCard, PostEditPayload } from '../../components/post-card/post-card';
import { Comment, Post, ReactionType } from '../../models/post.models';
import { PostService } from '../../services/post.service';

@Component({
  selector: 'app-post-detail',
  imports: [FormsModule, RouterLink, DatePipe, PostCard],
  templateUrl: './post-detail.html',
  styleUrl: './post-detail.css',
})
export class PostDetail implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly postsApi = inject(PostService);
  private readonly teamsApi = inject(TeamService);
  private readonly platformId = inject(PLATFORM_ID);
  readonly auth = inject(AuthService);

  readonly post = signal<Post | null>(null);
  readonly comments = signal<Comment[]>([]);
  readonly myTeams = signal<Team[]>([]);
  readonly loading = signal(true);
  readonly submitting = signal(false);
  readonly savingEdit = signal(false);
  readonly editing = signal(false);
  readonly error = signal<string | null>(null);
  readonly toast = signal<string | null>(null);

  commentText = '';

  ngOnInit(): void {
    if (!isPlatformBrowser(this.platformId)) return;

    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) {
      this.router.navigate(['/feed']);
      return;
    }
    this.loadMyTeams();
    this.load(id);
  }

  loadMyTeams(): void {
    this.teamsApi.listMine().subscribe({
      next: (teams) => this.myTeams.set(teams),
      error: () => this.myTeams.set([]),
    });
  }

  companyLabel(): string {
    const companies = this.auth.getUser()?.companies;
    return companies?.length ? companies[0].name : 'your company';
  }

  load(id: number): void {
    this.loading.set(true);
    this.error.set(null);

    this.postsApi.getById(id).subscribe({
      next: (post) => {
        this.post.set(post);
        this.loading.set(false);
        this.loadComments(id);
      },
      error: (err) => {
        this.error.set(err?.error?.message ?? 'Post not found');
        this.loading.set(false);
      },
    });
  }

  loadComments(postId: number): void {
    this.postsApi.listComments(postId).subscribe({
      next: (comments) => this.comments.set(comments),
      error: (err) => this.error.set(err?.error?.message ?? 'Could not load comments'),
    });
  }

  submitComment(): void {
    const post = this.post();
    const text = this.commentText.trim();
    if (!post || !text || this.submitting()) return;

    this.submitting.set(true);
    this.postsApi.addComment(post.id, { content: text }).subscribe({
      next: (comment) => {
        this.comments.update((list) => [...list, comment]);
        this.post.update((p) => (p ? { ...p, commentCount: p.commentCount + 1 } : p));
        this.commentText = '';
        this.submitting.set(false);
      },
      error: (err) => {
        this.error.set(err?.error?.message ?? 'Could not add comment');
        this.submitting.set(false);
      },
    });
  }

  deleteComment(comment: Comment): void {
    if (!confirm('Delete this comment?')) return;
    this.postsApi.deleteComment(comment.id).subscribe({
      next: () => {
        this.comments.update((list) => list.filter((c) => c.id !== comment.id));
        this.post.update((p) =>
          p ? { ...p, commentCount: Math.max(0, p.commentCount - 1) } : p
        );
      },
      error: (err) => this.error.set(err?.error?.message ?? 'Could not delete comment'),
    });
  }

  onReact(event: { post: Post; type: ReactionType }): void {
    this.postsApi.react(event.post.id, event.type).subscribe({
      next: (updated) => this.post.set(updated),
      error: (err) => this.error.set(err?.error?.message ?? 'Could not update reaction'),
    });
  }

  onSaveToggle(post: Post): void {
    const req$ = post.savedByMe ? this.postsApi.unsave(post.id) : this.postsApi.save(post.id);
    req$.subscribe({
      next: (updated) => {
        this.post.set(updated);
        this.showToast(updated.savedByMe ? 'Saved' : 'Removed from saved');
      },
      error: (err) => this.error.set(err?.error?.message ?? 'Could not update saved post'),
    });
  }

  async onShare(post: Post): Promise<void> {
    const url = `${window.location.origin}/feed/${post.id}`;
    try {
      await navigator.clipboard.writeText(url);
      this.showToast('Link copied');
    } catch {
      this.showToast(url);
    }
  }

  onEdit(_post: Post): void {
    this.editing.set(true);
  }

  cancelEdit(): void {
    this.editing.set(false);
    this.savingEdit.set(false);
  }

  onSaveEdit(payload: PostEditPayload): void {
    const post = this.post();
    if (!post || this.savingEdit()) return;

    this.savingEdit.set(true);
    this.error.set(null);

    this.postsApi.update(post.id, payload).subscribe({
      next: (updated) => {
        this.post.set(updated);
        this.editing.set(false);
        this.savingEdit.set(false);
        this.showToast('Post updated');
      },
      error: (err) => {
        this.error.set(err?.error?.message ?? 'Could not update post');
        this.savingEdit.set(false);
      },
    });
  }

  onDelete(post: Post): void {
    if (!confirm('Delete this post?')) return;
    this.postsApi.delete(post.id).subscribe({
      next: () => this.router.navigate(['/feed']),
      error: (err) => this.error.set(err?.error?.message ?? 'Could not delete post'),
    });
  }

  initials(comment: Comment): string {
    return `${comment.authorFirstName?.charAt(0) ?? ''}${comment.authorLastName?.charAt(0) ?? ''}`.toUpperCase();
  }

  private showToast(message: string): void {
    this.toast.set(message);
    setTimeout(() => {
      if (this.toast() === message) this.toast.set(null);
    }, 2200);
  }
}
