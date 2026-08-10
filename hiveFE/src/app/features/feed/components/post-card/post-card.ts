import { DatePipe } from '@angular/common';
import { Component, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { inject } from '@angular/core';
import { Team } from '../../../company/models/team.models';
import { Post, ReactionType, VisibilityType } from '../../models/post.models';
import { AttachmentResponse } from '../../../../shared/models/attachment.models';
import { AttachmentService } from '../../../../core/services/attachment.service';
import { AuthImage } from '../../../../shared/components/auth-image/auth-image';

@Component({
  selector: 'app-post-card',
  imports: [RouterLink, DatePipe, FormsModule, AuthImage],
  templateUrl: './post-card.html',
  styleUrl: './post-card.css',
})
export class PostCard {
  private readonly attachmentService = inject(AttachmentService);

  post = input.required<Post>();
  editing = input(false);
  saving = input(false);
  teams = input<Team[]>([]);
  companyName = input('your company');

  react = output<{ post: Post; type: ReactionType }>();
  saveToggle = output<Post>();
  share = output<Post>();
  edit = output<Post>();
  remove = output<Post>();
  saveEdit = output<{ content: string; visibilityType: VisibilityType; teamId: number | null }>();
  cancelEdit = output<void>();

  /** Parent (FeedHome / PostDetail) syncs its own copy of the post from this. */
  attachmentsChanged = output<{ post: Post; attachments: AttachmentResponse[] }>();

  editContent = '';
  editVisibility: VisibilityType = 'COMPANY';
  editTeamId: number | null = null;

  attachmentUploading = signal(false);
  attachmentError = signal('');

  reactions: { type: ReactionType; emoji: string }[] = [
    { type: 'LIKE', emoji: '👍' },
    { type: 'LOVE', emoji: '❤️' },
    { type: 'LAUGHING', emoji: '😂' },
    { type: 'SAD', emoji: '😢' },
    { type: 'ANGRY', emoji: '😡' },
  ];

  startEdit(): void {
    const p = this.post();
    this.editContent = p.content;
    this.editVisibility = p.visibilityType;
    this.editTeamId = p.teamId ?? null;
    this.edit.emit(p);
  }

  submitEdit(): void {
    this.saveEdit.emit({
      content: this.editContent.trim(),
      visibilityType: this.editVisibility,
      teamId: this.editVisibility === 'TEAM' ? this.editTeamId : null,
    });
  }

  onFileSelected(input: HTMLInputElement): void {
    const file = input.files?.[0];
    input.value = '';
    if (!file) return;

    const validationError = this.attachmentService.validate(file, 'POST');
    if (validationError) {
      this.attachmentError.set(validationError);
      return;
    }

    this.attachmentError.set('');
    this.attachmentUploading.set(true);
    const post = this.post();

    this.attachmentService.uploadPostAttachment(post.id, file).subscribe({
      next: (attachment) => {
        this.attachmentUploading.set(false);
        const updated = [...post.attachments, attachment];
        this.attachmentsChanged.emit({ post, attachments: updated });
      },
      error: (err) => {
        this.attachmentUploading.set(false);
        this.attachmentError.set(err.error?.message ?? 'Failed to attach file');
      },
    });
  }

  removeAttachment(attachment: AttachmentResponse): void {
    const post = this.post();
    this.attachmentError.set('');
    this.attachmentService.deletePostAttachment(post.id, attachment.id).subscribe({
      next: () => {
        const updated = post.attachments.filter((a) => a.id !== attachment.id);
        this.attachmentsChanged.emit({ post, attachments: updated });
      },
      error: (err) => this.attachmentError.set(err.error?.message ?? 'Failed to remove attachment'),
    });
  }

  openAttachment(attachment: AttachmentResponse): void {
    this.attachmentService.open(attachment.url);
  }
}