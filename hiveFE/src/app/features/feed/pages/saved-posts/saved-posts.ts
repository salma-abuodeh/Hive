import { isPlatformBrowser } from '@angular/common';
import { Component, OnInit, PLATFORM_ID, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { PostCard } from '../../components/post-card/post-card';
import { Post, ReactionType } from '../../models/post.models';
import { PostService } from '../../services/post.service';

@Component({
  selector: 'app-saved-posts',
  imports: [PostCard],
  templateUrl: './saved-posts.html',
  styleUrl: './saved-posts.css',
})
export class SavedPosts implements OnInit {
  private readonly postsApi = inject(PostService);
  private readonly router = inject(Router);
  private readonly platformId = inject(PLATFORM_ID);

  readonly posts = signal<Post[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly toast = signal<string | null>(null);
  readonly page = signal(0);
  readonly lastPage = signal(true);

  ngOnInit(): void {
    if (!isPlatformBrowser(this.platformId)) return;
    this.load(true);
  }

  load(reset = false): void {
    if (this.loading()) return;
    const nextPage = reset ? 0 : this.page() + 1;
    this.loading.set(true);
    this.error.set(null);

    this.postsApi.listSaved(nextPage, 10).subscribe({
      next: (res) => {
        this.posts.set(reset ? res.content : [...this.posts(), ...res.content]);
        this.page.set(res.number);
        this.lastPage.set(res.last);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err?.error?.message ?? 'Could not load saved posts');
        this.loading.set(false);
      },
    });
  }

  onReact(event: { post: Post; type: ReactionType }): void {
    this.postsApi.react(event.post.id, event.type).subscribe({
      next: (updated) => this.replacePost(updated),
      error: (err) => this.error.set(err?.error?.message ?? 'Could not update reaction'),
    });
  }

  onSaveToggle(post: Post): void {
    this.postsApi.unsave(post.id).subscribe({
      next: () => {
        this.posts.update((list) => list.filter((p) => p.id !== post.id));
        this.showToast('Removed from saved');
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

  onEdit(post: Post): void {
    this.router.navigate(['/feed'], { state: { editPost: post } });
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
