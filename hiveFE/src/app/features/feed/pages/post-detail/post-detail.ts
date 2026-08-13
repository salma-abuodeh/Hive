import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import { TeamService } from '../../../../core/services/team.service';
import { Team } from '../../../company/models/team.models';
import { PostCard } from '../../components/post-card/post-card';
import { AttachmentResponse } from '../../../../shared/models/attachment.models';
import { Post, ReactionType } from '../../models/post.models';
import { PostService } from '../../services/post.service';

@Component({
  selector: 'app-post-detail',
  imports: [PostCard],
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
  myTeams = signal<Team[]>([]);
  loading = signal(true);
  busy = signal(false);
  error = signal('');
  editing = false;

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

  onShare(): void {
    // Copying + social share links are now handled entirely inside PostCard's share menu.
  }

  onAttachmentsChanged(e: { post: Post; attachments: AttachmentResponse[] }): void {
    this.post.update((p) => (p && p.id === e.post.id ? { ...p, attachments: e.attachments } : p));
  }

  onCommentCountChanged(e: { post: Post; delta: number }): void {
    this.post.update((p) => (p && p.id === e.post.id ? { ...p, commentCount: p.commentCount + e.delta } : p));
  }
}