import { isPlatformBrowser } from '@angular/common';
import { Component, OnInit, PLATFORM_ID, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../../../core/services/auth.service';
import { TeamService } from '../../../../core/services/team.service';
import { Team } from '../../../company/models/team.models';
import { PostCard } from '../../components/post-card/post-card';
import { CreatePostRequest, Post, ReactionType, VisibilityType } from '../../models/post.models';
import { PostService } from '../../services/post.service';

@Component({
  selector: 'app-feed-home',
  imports: [FormsModule, PostCard],
  templateUrl: './feed-home.html',
  styleUrl: './feed-home.css',
})
export class FeedHome implements OnInit {
  private readonly postsApi = inject(PostService);
  private readonly teamsApi = inject(TeamService);
  private readonly platformId = inject(PLATFORM_ID);
  readonly auth = inject(AuthService);

  readonly posts = signal<Post[]>([]);
  readonly myTeams = signal<Team[]>([]);
  readonly loading = signal(false);
  readonly publishing = signal(false);
  readonly error = signal<string | null>(null);
  readonly toast = signal<string | null>(null);
  readonly page = signal(0);
  readonly lastPage = signal(true);
  readonly editingId = signal<number | null>(null);

  content = '';
  visibilityType: VisibilityType = 'COMPANY';
  teamId: number | null = null;

  ngOnInit(): void {
    if (!isPlatformBrowser(this.platformId)) return;

    const editPost = history.state?.['editPost'] as Post | undefined;
    if (editPost) {
      this.onEdit(editPost);
      history.replaceState({ ...history.state, editPost: null }, '');
    }
    this.loadMyTeams();
    this.loadFeed(true);
  }

  loadMyTeams(): void {
    this.teamsApi.listMine().subscribe({
      next: (teams) => this.myTeams.set(teams),
      error: () => this.myTeams.set([]),
    });
  }

  onVisibilityChange(): void {
    if (this.visibilityType !== 'TEAM') {
      this.teamId = null;
    } else if (this.myTeams().length && this.teamId == null) {
      this.teamId = this.myTeams()[0].id;
    }
  }

  companyLabel(): string {
    const companies = this.auth.getUser()?.companies;
    return companies?.length ? companies[0].name : 'your company';
  }

  initials(): string {
    const user = this.auth.getUser();
    if (!user) return '?';
    return `${user.firstName?.charAt(0) ?? ''}${user.lastName?.charAt(0) ?? ''}`.toUpperCase();
  }

  loadFeed(reset = false): void {
    if (this.loading()) return;
    const nextPage = reset ? 0 : this.page() + 1;
    this.loading.set(true);
    this.error.set(null);

    this.postsApi.listFeed(nextPage, 10).subscribe({
      next: (res) => {
        this.posts.set(reset ? res.content : [...this.posts(), ...res.content]);
        this.page.set(res.number);
        this.lastPage.set(res.last);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err?.error?.message ?? 'Could not load the feed');
        this.loading.set(false);
      },
    });
  }

  publish(): void {
    const text = this.content.trim();
    if (!text || this.publishing()) return;

    if (this.visibilityType === 'TEAM' && this.teamId == null) {
      this.error.set('Pick a team for team-only posts');
      return;
    }

    const payload: CreatePostRequest = {
      content: text,
      visibilityType: this.visibilityType,
      teamId: this.visibilityType === 'TEAM' ? this.teamId : null,
    };

    const editId = this.editingId();
    this.publishing.set(true);
    this.error.set(null);

    const req$ = editId
      ? this.postsApi.update(editId, payload)
      : this.postsApi.create(payload);

    req$.subscribe({
      next: (post) => {
        if (editId) {
          this.posts.update((list) => list.map((p) => (p.id === post.id ? post : p)));
          this.showToast('Post updated');
        } else {
          this.posts.update((list) => [post, ...list]);
          this.showToast('Post published');
        }
        this.content = '';
        this.visibilityType = 'COMPANY';
        this.teamId = null;
        this.editingId.set(null);
        this.publishing.set(false);
      },
      error: (err) => {
        this.error.set(err?.error?.message ?? 'Could not publish post');
        this.publishing.set(false);
      },
    });
  }

  cancelEdit(): void {
    this.editingId.set(null);
    this.content = '';
    this.visibilityType = 'COMPANY';
    this.teamId = null;
  }

  onEdit(post: Post): void {
    this.editingId.set(post.id);
    this.content = post.content;
    this.visibilityType = post.visibilityType;
    this.teamId = post.teamId ?? null;
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  onDelete(post: Post): void {
    if (!confirm('Delete this post?')) return;
    this.postsApi.delete(post.id).subscribe({
      next: () => {
        this.posts.update((list) => list.filter((p) => p.id !== post.id));
        this.showToast('Post deleted');
      },
      error: (err) => this.error.set(err?.error?.message ?? 'Could not delete post'),
    });
  }

  onReact(event: { post: Post; type: ReactionType }): void {
    this.postsApi.react(event.post.id, event.type).subscribe({
      next: (updated) => this.replacePost(updated),
      error: (err) => this.error.set(err?.error?.message ?? 'Could not update reaction'),
    });
  }

  onSaveToggle(post: Post): void {
    const req$ = post.savedByMe ? this.postsApi.unsave(post.id) : this.postsApi.save(post.id);
    req$.subscribe({
      next: (updated) => {
        this.replacePost(updated);
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

  private replacePost(updated: Post): void {
    this.posts.update((list) => list.map((p) => (p.id === updated.id ? updated : p)));
  }

  private showToast(message: string): void {
    this.toast.set(message);
    setTimeout(() => {
      if (this.toast() === message) this.toast.set(null);
    }, 2200);
  }
}
