import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../../../core/services/auth.service';
import { TeamService } from '../../../../core/services/team.service';
import { Team } from '../../../company/models/team.models';
import { PostCard } from '../../components/post-card/post-card';
import { Post, ReactionType, VisibilityType } from '../../models/post.models';
import { PostService } from '../../services/post.service';

@Component({
  selector: 'app-feed-home',
  imports: [FormsModule, PostCard],
  templateUrl: './feed-home.html',
  styleUrl: './feed-home.css',
})
export class FeedHome implements OnInit {
  private postsApi = inject(PostService);
  private teamsApi = inject(TeamService);
  auth = inject(AuthService);

  posts = signal<Post[]>([]);
  myTeams = signal<Team[]>([]);
  loading = signal(false);
  busy = signal(false);
  error = signal('');
  toast = signal('');
  page = 0;
  lastPage = true;
  editingId: number | null = null;

  content = '';
  visibilityType: VisibilityType = 'COMPANY';
  teamId: number | null = null;

  ngOnInit(): void {
    const editPost = history.state?.['editPost'] as Post | undefined;
    if (editPost?.id) {
      this.editingId = editPost.id;
    }

    this.teamsApi.listMine().subscribe({
      next: (teams) => this.myTeams.set(teams),
      error: () => this.myTeams.set([]),
    });
    this.loadFeed(true);
  }

  companyName(): string {
    return this.auth.getUser()?.companies?.[0]?.name ?? 'your company';
  }

  initials(): string {
    const u = this.auth.getUser();
    if (!u) return '?';
    return `${u.firstName?.[0] ?? ''}${u.lastName?.[0] ?? ''}`.toUpperCase();
  }

  onVisibilityChange(): void {
    if (this.visibilityType !== 'TEAM') {
      this.teamId = null;
    } else if (this.myTeams().length && this.teamId == null) {
      this.teamId = this.myTeams()[0].id;
    }
  }

  loadFeed(reset = false): void {
    if (this.loading()) return;
    const nextPage = reset ? 0 : this.page + 1;
    this.loading.set(true);
    this.error.set('');

    this.postsApi.listFeed(nextPage).subscribe({
      next: (res) => {
        this.posts.set(reset ? res.content : [...this.posts(), ...res.content]);
        this.page = res.number;
        this.lastPage = res.last;
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err.error?.message ?? 'Could not load feed');
        this.loading.set(false);
      },
    });
  }

  publish(): void {
    const text = this.content.trim();
    if (!text || this.busy()) return;

    this.busy.set(true);
    this.postsApi
      .create({
        content: text,
        visibilityType: this.visibilityType,
        teamId: this.visibilityType === 'TEAM' ? this.teamId : null,
      })
      .subscribe({
        next: (post) => {
          this.posts.update((list) => [post, ...list]);
          this.content = '';
          this.visibilityType = 'COMPANY';
          this.teamId = null;
          this.busy.set(false);
          this.toast.set('Published');
        },
        error: (err) => {
          this.error.set(err.error?.message ?? 'Could not publish');
          this.busy.set(false);
        },
      });
  }

  onEdit(post: Post): void {
    this.editingId = post.id;
  }

  cancelEdit(): void {
    this.editingId = null;
  }

  onSaveEdit(data: { content: string; visibilityType: VisibilityType; teamId: number | null }): void {
    if (!this.editingId || this.busy()) return;
    this.busy.set(true);

    this.postsApi.update(this.editingId, data).subscribe({
      next: (post) => {
        this.posts.update((list) => list.map((p) => (p.id === post.id ? post : p)));
        this.editingId = null;
        this.busy.set(false);
        this.toast.set('Updated');
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
      next: () => {
        this.posts.update((list) => list.filter((p) => p.id !== post.id));
        if (this.editingId === post.id) this.editingId = null;
      },
      error: (err) => this.error.set(err.error?.message ?? 'Could not delete'),
    });
  }

  onReact(e: { post: Post; type: ReactionType }): void {
    this.postsApi.react(e.post.id, e.type).subscribe({
      next: (updated) => {
        this.posts.update((list) => list.map((p) => (p.id === updated.id ? updated : p)));
      },
      error: (err) => this.error.set(err.error?.message ?? 'Could not react'),
    });
  }

  onSaveToggle(post: Post): void {
    const call = post.savedByMe ? this.postsApi.unsave(post.id) : this.postsApi.save(post.id);
    call.subscribe({
      next: (updated) => {
        this.posts.update((list) => list.map((p) => (p.id === updated.id ? updated : p)));
      },
      error: (err) => this.error.set(err.error?.message ?? 'Could not save'),
    });
  }

  onShare(post: Post): void {
    navigator.clipboard.writeText(`${window.location.origin}/feed/${post.id}`);
    this.toast.set('Link copied');
  }
}
