import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import { TeamService } from '../../../../core/services/team.service';
import { Team } from '../../../company/models/team.models';
import { PostCard } from '../../components/post-card/post-card';
import { Comment, Post, ReactionType } from '../../models/post.models';
import { PostService } from '../../services/post.service';

@Component({
  selector: 'app-post-detail',
  imports: [FormsModule, RouterLink, DatePipe, PostCard],
  templateUrl: './post-detail.html',
  styleUrl: './post-detail.css',
})
export class PostDetail implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private postsApi = inject(PostService);
  private teamsApi = inject(TeamService);
  auth = inject(AuthService);

  post = signal<Post | null>(null);
  comments = signal<Comment[]>([]);
  myTeams = signal<Team[]>([]);
  loading = signal(true);
  busy = signal(false);
  error = signal('');
  editing = false;
  commentText = '';

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) {
      this.router.navigate(['/feed']);
      return;
    }

    this.teamsApi.listMine().subscribe({
      next: (teams) => this.myTeams.set(teams),
      error: () => this.myTeams.set([]),
    });

    this.postsApi.getById(id).subscribe({
      next: (post) => {
        this.post.set(post);
        this.loading.set(false);
        this.postsApi.listComments(id).subscribe({
          next: (comments) => this.comments.set(comments),
        });
      },
      error: () => {
        this.error.set('Post not found');
        this.loading.set(false);
      },
    });
  }

  companyName(): string {
    return this.auth.getUser()?.companies?.[0]?.name ?? 'your company';
  }

  addComment(): void {
    const post = this.post();
    const text = this.commentText.trim();
    if (!post || !text) return;

    this.postsApi.addComment(post.id, text).subscribe({
      next: (comment) => {
        this.comments.update((list) => [...list, comment]);
        this.post.update((p) => (p ? { ...p, commentCount: p.commentCount + 1 } : p));
        this.commentText = '';
      },
      error: (err) => this.error.set(err.error?.message ?? 'Could not comment'),
    });
  }

  deleteComment(comment: Comment): void {
    if (!confirm('Delete comment?')) return;
    this.postsApi.deleteComment(comment.id).subscribe({
      next: () => {
        this.comments.update((list) => list.filter((c) => c.id !== comment.id));
        this.post.update((p) => (p ? { ...p, commentCount: p.commentCount - 1 } : p));
      },
    });
  }

  onEdit(post: Post): void {
    this.editing = true;
  }

  cancelEdit(): void {
    this.editing = false;
  }

  onSaveEdit(data: { content: string; visibilityType: Post['visibilityType']; teamId: number | null }): void {
    const post = this.post();
    if (!post) return;

    this.busy.set(true);
    this.postsApi.update(post.id, data).subscribe({
      next: (updated) => {
        this.post.set(updated);
        this.editing = false;
        this.busy.set(false);
      },
      error: (err) => {
        this.error.set(err.error?.message ?? 'Could not update');
        this.busy.set(false);
      },
    });
  }

  onDelete(post: Post): void {
    if (!confirm('Delete this post?')) return;
    this.postsApi.delete(post.id).subscribe({
      next: () => this.router.navigate(['/feed']),
    });
  }

  onReact(e: { post: Post; type: ReactionType }): void {
    this.postsApi.react(e.post.id, e.type).subscribe({
      next: (updated) => this.post.set(updated),
    });
  }

  onSaveToggle(post: Post): void {
    const call = post.savedByMe ? this.postsApi.unsave(post.id) : this.postsApi.save(post.id);
    call.subscribe({ next: (updated) => this.post.set(updated) });
  }

  onShare(post: Post): void {
    navigator.clipboard.writeText(`${window.location.origin}/feed/${post.id}`);
  }
}
