import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import { PostCard } from '../../components/post-card/post-card';
import { CreatePostRequest, Post, VisibilityType } from '../../models/post.models';
import { PostService } from '../../services/post.service';

@Component({
  selector: 'app-feed-home',
  imports: [FormsModule, PostCard],
  templateUrl: './feed-home.html',
  styleUrl: './feed-home.css',
})
export class FeedHome implements OnInit {
  private readonly postsApi = inject(PostService);
  private readonly router = inject(Router);
  readonly auth = inject(AuthService);

  readonly posts = signal<Post[]>([]);
  readonly loading = signal(false);
  readonly publishing = signal(false);
  readonly error = signal<string | null>(null);
  readonly toast = signal<string | null>(null);
  readonly page = signal(0);
  readonly lastPage = signal(true);
  readonly editingId = signal<number | null>(null);

  content = '';
  visibilityType: VisibilityType = 'COMPANY';

  ngOnInit(): void {
    this.loadFeed(true);
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

    const payload: CreatePostRequest = {
      content: text,
      visibilityType: this.visibilityType,
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
  }

  onEdit(post: Post): void {
    this.editingId.set(post.id);
    this.content = post.content;
    this.visibilityType = post.visibilityType;
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

  onLikeToggle(post: Post): void {
    const req$ = post.likedByMe ? this.postsApi.unlike(post.id) : this.postsApi.like(post.id);
    req$.subscribe({
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
