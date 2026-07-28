import { Component, OnInit, inject, signal } from '@angular/core';
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
  private postsApi = inject(PostService);
  private router = inject(Router);

  posts = signal<Post[]>([]);
  loading = signal(false);
  error = signal('');
  page = 0;
  lastPage = true;

  ngOnInit(): void {
    this.load(true);
  }

  load(reset = false): void {
    if (this.loading()) return;
    const nextPage = reset ? 0 : this.page + 1;
    this.loading.set(true);

    this.postsApi.listSaved(nextPage).subscribe({
      next: (res) => {
        this.posts.set(reset ? res.content : [...this.posts(), ...res.content]);
        this.page = res.number;
        this.lastPage = res.last;
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err.error?.message ?? 'Could not load saved posts');
        this.loading.set(false);
      },
    });
  }

  onReact(e: { post: Post; type: ReactionType }): void {
    this.postsApi.react(e.post.id, e.type).subscribe({
      next: (updated) => {
        this.posts.update((list) => list.map((p) => (p.id === updated.id ? updated : p)));
      },
    });
  }

  onSaveToggle(post: Post): void {
    this.postsApi.unsave(post.id).subscribe({
      next: () => this.posts.update((list) => list.filter((p) => p.id !== post.id)),
    });
  }

  onShare(post: Post): void {
    navigator.clipboard.writeText(`${window.location.origin}/feed/${post.id}`);
  }

  onEdit(post: Post): void {
    this.router.navigate(['/feed'], { state: { editPost: post } });
  }

  onDelete(post: Post): void {
    if (!confirm('Delete this post?')) return;
    this.postsApi.delete(post.id).subscribe({
      next: () => this.posts.update((list) => list.filter((p) => p.id !== post.id)),
    });
  }
}
