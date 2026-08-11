import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Team } from '../../../company/models/team.models';
import { Post, ReactionType, VisibilityType, Comment } from '../../models/post.models';
import { AttachmentResponse } from '../../../../shared/models/attachment.models';
import { AttachmentService } from '../../../../core/services/attachment.service';
import { AuthImage } from '../../../../shared/components/auth-image/auth-image';
import { PostService } from '../../services/post.service';

@Component({
  selector: 'app-post-card',
  imports: [DatePipe, FormsModule, AuthImage],
  templateUrl: './post-card.html',
  styleUrl: './post-card.css',
})
export class PostCard implements OnInit {
  private readonly attachmentService = inject(AttachmentService);
  private readonly postsApi = inject(PostService);

  post = input.required<Post>();
  editing = input(false);
  saving = input(false);
  teams = input<Team[]>([]);
  companyName = input('your company');
  /** Post-detail page passes true so the permalink view lands with comments already open. */
  autoExpandComments = input(false);

  react = output<{ post: Post; type: ReactionType }>();
  saveToggle = output<Post>();
  share = output<Post>();
  edit = output<Post>();
  remove = output<Post>();
  saveEdit = output<{ content: string; visibilityType: VisibilityType; teamId: number | null }>();
  cancelEdit = output<void>();

  /** Parent (FeedHome / PostDetail) syncs its own copy of the post from these. */
  attachmentsChanged = output<{ post: Post; attachments: AttachmentResponse[] }>();
  commentCountChanged = output<{ post: Post; delta: number }>();

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

  // ---------- Inline comments panel ----------

  commentsOpen = signal(false);
  commentsLoading = signal(false);
  comments = signal<Comment[]>([]);
  commentText = '';
  commentSubmitting = signal(false);
  commentError = signal('');

  stagedCommentFiles: File[] = [];
  stagedCommentPreviews = signal<{ file: File; previewUrl: string | null }[]>([]);
  commentAttachError = signal('');

  ngOnInit(): void {
    if (this.autoExpandComments()) {
      this.commentsOpen.set(true);
      this.loadComments();
    }
  }

  toggleComments(): void {
    const wasOpen = this.commentsOpen();
    this.commentsOpen.set(!wasOpen);
    if (!wasOpen && this.comments().length === 0) {
      this.loadComments();
    }
  }

  private loadComments(): void {
    this.commentsLoading.set(true);
    this.postsApi.listComments(this.post().id).subscribe({
      next: (comments) => {
        this.comments.set(comments);
        this.commentsLoading.set(false);
      },
      error: (err) => {
        this.commentError.set(err.error?.message ?? 'Could not load comments');
        this.commentsLoading.set(false);
      },
    });
  }

  addComment(): void {
    const text = this.commentText.trim();
    if (!text || this.commentSubmitting()) return;

    this.commentSubmitting.set(true);
    this.commentError.set('');
    this.postsApi.addComment(this.post().id, text).subscribe({
      next: (comment) => this.afterCommentCreated(comment),
      error: (err) => {
        this.commentError.set(err.error?.message ?? 'Could not comment');
        this.commentSubmitting.set(false);
      },
    });
  }

  private afterCommentCreated(comment: Comment): void {
    if (this.stagedCommentFiles.length === 0) {
      this.finishAddComment(comment);
      return;
    }
    this.uploadStagedCommentFiles(comment, [...this.stagedCommentFiles], []);
  }

  private uploadStagedCommentFiles(comment: Comment, remaining: File[], uploaded: AttachmentResponse[]): void {
    if (remaining.length === 0) {
      this.finishAddComment({ ...comment, attachments: uploaded });
      return;
    }
    const [next, ...rest] = remaining;
    this.attachmentService.uploadCommentAttachment(comment.id, next).subscribe({
      next: (attachment) => this.uploadStagedCommentFiles(comment, rest, [...uploaded, attachment]),
      error: (err) => {
        this.commentError.set(err.error?.message ?? 'Comment posted, but one attachment failed to upload');
        this.finishAddComment({ ...comment, attachments: uploaded });
      },
    });
  }

  private finishAddComment(comment: Comment): void {
    this.comments.update((list) => [...list, comment]);
    this.commentCountChanged.emit({ post: this.post(), delta: 1 });
    this.commentText = '';
    this.clearCommentStaging();
    this.commentSubmitting.set(false);
  }

  deleteComment(comment: Comment): void {
    if (!confirm('Delete comment?')) return;
    this.postsApi.deleteComment(comment.id).subscribe({
      next: () => {
        this.comments.update((list) => list.filter((c) => c.id !== comment.id));
        this.commentCountChanged.emit({ post: this.post(), delta: -1 });
      },
      error: (err) => this.commentError.set(err.error?.message ?? 'Could not delete comment'),
    });
  }

  onCommentFileSelected(input: HTMLInputElement): void {
    const file = input.files?.[0];
    input.value = '';
    if (!file) return;

    if (this.stagedCommentFiles.length >= 5) {
      this.commentAttachError.set('A comment can have at most 5 attachments');
      return;
    }

    const validationError = this.attachmentService.validate(file, 'COMMENT');
    if (validationError) {
      this.commentAttachError.set(validationError);
      return;
    }

    this.commentAttachError.set('');
    this.stagedCommentFiles.push(file);

    if (file.type.startsWith('image/')) {
      const reader = new FileReader();
      reader.onload = () => {
        this.stagedCommentPreviews.update((list) => [...list, { file, previewUrl: reader.result as string }]);
      };
      reader.readAsDataURL(file);
    } else {
      this.stagedCommentPreviews.update((list) => [...list, { file, previewUrl: null }]);
    }
  }

  removeStagedCommentFile(file: File): void {
    this.stagedCommentFiles = this.stagedCommentFiles.filter((f) => f !== file);
    this.stagedCommentPreviews.update((list) => list.filter((p) => p.file !== file));
  }

  private clearCommentStaging(): void {
    this.stagedCommentFiles = [];
    this.stagedCommentPreviews.set([]);
    this.commentAttachError.set('');
  }

  commentImages(comment: Comment): AttachmentResponse[] {
    return comment.attachments.filter((a) => a.attachmentType === 'IMAGE');
  }

  commentDocuments(comment: Comment): AttachmentResponse[] {
    return comment.attachments.filter((a) => a.attachmentType === 'DOCUMENT');
  }

  removeCommentAttachment(comment: Comment, attachment: AttachmentResponse): void {
    this.attachmentService.deleteCommentAttachment(comment.id, attachment.id).subscribe({
      next: () => {
        this.comments.update((list) =>
          list.map((c) =>
            c.id === comment.id
              ? { ...c, attachments: c.attachments.filter((a) => a.id !== attachment.id) }
              : c
          )
        );
      },
      error: (err) => this.commentError.set(err.error?.message ?? 'Failed to remove attachment'),
    });
  }

  openCommentAttachment(attachment: AttachmentResponse): void {
    this.attachmentService.open(attachment.url);
  }

  // ---------- Comment photo lightbox ----------

  lightboxComment = signal<Comment | null>(null);
  lightboxIndex = signal<number | null>(null);
  lightboxUrl = signal<string | null>(null);
  lightboxLoading = signal(false);

  lightboxHasMultiple(): boolean {
    const comment = this.lightboxComment();
    return comment ? this.commentImages(comment).length > 1 : false;
  }

  openCommentImage(comment: Comment, index: number): void {
    this.lightboxComment.set(comment);
    this.lightboxIndex.set(index);
    this.loadLightboxImage();
  }

  closeLightbox(): void {
    this.revokeLightboxUrl();
    this.lightboxComment.set(null);
    this.lightboxIndex.set(null);
  }

  prevLightboxImage(): void {
    const comment = this.lightboxComment();
    const idx = this.lightboxIndex();
    if (!comment || idx == null) return;
    const len = this.commentImages(comment).length;
    this.lightboxIndex.set((idx - 1 + len) % len);
    this.loadLightboxImage();
  }

  nextLightboxImage(): void {
    const comment = this.lightboxComment();
    const idx = this.lightboxIndex();
    if (!comment || idx == null) return;
    const len = this.commentImages(comment).length;
    this.lightboxIndex.set((idx + 1) % len);
    this.loadLightboxImage();
  }

  private loadLightboxImage(): void {
    const comment = this.lightboxComment();
    const idx = this.lightboxIndex();
    const attachment = comment && idx != null ? this.commentImages(comment)[idx] : null;
    if (!attachment) return;

    this.revokeLightboxUrl();
    this.lightboxLoading.set(true);
    this.attachmentService.getObjectUrl(attachment.url).subscribe({
      next: (url) => {
        this.lightboxUrl.set(url);
        this.lightboxLoading.set(false);
      },
      error: () => this.lightboxLoading.set(false),
    });
  }

  private revokeLightboxUrl(): void {
    const current = this.lightboxUrl();
    if (current) {
      URL.revokeObjectURL(current);
    }
    this.lightboxUrl.set(null);
  }

  // ---------- Post editing / actions ----------

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

  // ---------- Post attachments ----------

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

  images(): AttachmentResponse[] {
    return this.post().attachments.filter((a) => a.attachmentType === 'IMAGE');
  }

  documents(): AttachmentResponse[] {
    return this.post().attachments.filter((a) => a.attachmentType === 'DOCUMENT');
  }

  visibleImages(): AttachmentResponse[] {
    return this.images().slice(0, 4);
  }

  extraImageCount(): number {
    return Math.max(0, this.images().length - 4);
  }

  // ---------- Post photo lightbox ----------

  postLightboxIndex = signal<number | null>(null);
  postLightboxUrl = signal<string | null>(null);
  postLightboxLoading = signal(false);

  openImage(index: number): void {
    this.postLightboxIndex.set(index);
    this.loadPostLightboxImage();
  }

  closePostLightbox(): void {
    this.revokePostLightboxUrl();
    this.postLightboxIndex.set(null);
  }

  prevImage(): void {
    const idx = this.postLightboxIndex();
    if (idx == null) return;
    const len = this.images().length;
    this.postLightboxIndex.set((idx - 1 + len) % len);
    this.loadPostLightboxImage();
  }

  nextImage(): void {
    const idx = this.postLightboxIndex();
    if (idx == null) return;
    const len = this.images().length;
    this.postLightboxIndex.set((idx + 1) % len);
    this.loadPostLightboxImage();
  }

  private loadPostLightboxImage(): void {
    const idx = this.postLightboxIndex();
    const attachment = idx != null ? this.images()[idx] : null;
    if (!attachment) return;

    this.revokePostLightboxUrl();
    this.postLightboxLoading.set(true);
    this.attachmentService.getObjectUrl(attachment.url).subscribe({
      next: (url) => {
        this.postLightboxUrl.set(url);
        this.postLightboxLoading.set(false);
      },
      error: () => this.postLightboxLoading.set(false),
    });
  }

  private revokePostLightboxUrl(): void {
    const current = this.postLightboxUrl();
    if (current) {
      URL.revokeObjectURL(current);
    }
    this.postLightboxUrl.set(null);
  }
}